package ru.adiaphora.platform.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import ru.adiaphora.platform.auth.domain.User;
import ru.adiaphora.platform.auth.domain.UserRepository;
import ru.adiaphora.platform.support.AbstractIntegrationTest;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository users;

    @Test
    void registerLoginAndFetchProfile() throws Exception {
        String email = unique();
        register(email, "Password123!");
        String token = login(email, "Password123!");

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordIsHashedNotStoredInPlaintext() throws Exception {
        String email = unique();
        register(email, "Password123!");

        User stored = users.findByEmail(email).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(stored.passwordHash()).isNotEqualTo("Password123!");
    }

    @Test
    void disabledUserCannotLogIn() throws Exception {
        String email = unique();
        register(email, "Password123!");
        User user = users.findByEmail(email).orElseThrow();
        user.disable();
        users.save(user);

        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", "Password123!"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        String email = unique();
        register(email, "Password123!");

        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", "WrongPass123!"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedFailedLoginsLockTheAccountEvenWithTheCorrectPassword() throws Exception {
        String email = unique();
        register(email, "Password123!");

        // Exhaust the lockout threshold (default 5) with wrong passwords.
        for (int i = 0; i < 5; i++) {
            attemptLogin(email, "WrongPass123!").andExpect(status().isUnauthorized());
        }

        // The account is now locked: even the correct password is rejected.
        attemptLogin(email, "Password123!").andExpect(status().isUnauthorized());
    }

    private ResultActions attemptLogin(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String unique() {
        return "user-" + UUID.randomUUID() + "@example.test";
    }
}
