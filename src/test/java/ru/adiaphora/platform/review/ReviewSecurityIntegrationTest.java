package ru.adiaphora.platform.review;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.adiaphora.platform.auth.domain.User;
import ru.adiaphora.platform.auth.domain.UserRepository;
import ru.adiaphora.platform.auth.domain.UserRole;
import ru.adiaphora.platform.support.AbstractIntegrationTest;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the review endpoints' role matrix: normal users are excluded entirely; only ADMIN assigns;
 * only LAWYER/ADMIN decide; AUDITOR (read-only) cannot mutate. Authorization is checked without a
 * pre-existing review — {@code @PreAuthorize} denials return 403 before the handler runs.
 */
class ReviewSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository users;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void normalUserCannotListReviews() throws Exception {
        String token = normalUser();
        mockMvc.perform(get("/api/v1/reviews").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/reviews")).andExpect(status().isUnauthorized());
    }

    @Test
    void operatorCannotApprove() throws Exception {
        String token = staffUser(UserRole.OPERATOR);
        mockMvc.perform(post("/api/v1/reviews/" + UUID.randomUUID() + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "ok"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditorCannotAssign() throws Exception {
        String token = staffUser(UserRole.AUDITOR);
        mockMvc.perform(post("/api/v1/reviews/" + UUID.randomUUID() + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("assigneeId", UUID.randomUUID()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void lawyerIsAuthorizedToDecideButReviewIsNotFound() throws Exception {
        String token = staffUser(UserRole.LAWYER);
        mockMvc.perform(post("/api/v1/reviews/" + UUID.randomUUID() + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "insufficient evidence"))))
                .andExpect(status().isNotFound());
    }

    private String normalUser() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.test";
        register(email, "Password123!");
        return login(email, "Password123!");
    }

    private String staffUser(UserRole role) throws Exception {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.test";
        users.save(User.register(UUID.randomUUID(), email, passwordEncoder.encode("Password123!"), role));
        return login(email, "Password123!");
    }
}
