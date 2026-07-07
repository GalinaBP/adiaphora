package ru.adiaphora.platform.audit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.adiaphora.platform.auth.domain.User;
import ru.adiaphora.platform.auth.domain.UserRepository;
import ru.adiaphora.platform.auth.domain.UserRole;
import ru.adiaphora.platform.support.AbstractIntegrationTest;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Access-control tests for the audit route family, which the URL security matrix restricts to
 * ADMIN/AUDITOR. Complements {@code ReviewSecurityIntegrationTest}: unauthenticated callers get 401,
 * a normal USER gets 403, and the two privileged roles are admitted (200).
 */
class AuditSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository users;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void unauthenticatedIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events")).andExpect(status().isUnauthorized());
    }

    @Test
    void normalUserIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events").header(HttpHeaders.AUTHORIZATION, bearer(normalUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListAuditEvents() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events")
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffUser(UserRole.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    void auditorCanListAuditEvents() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events")
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffUser(UserRole.AUDITOR))))
                .andExpect(status().isOk());
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
