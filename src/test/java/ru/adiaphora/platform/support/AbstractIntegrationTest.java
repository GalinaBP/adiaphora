package ru.adiaphora.platform.support;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.adiaphora.platform.common.web.CorrelationIdFilter;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base class for integration tests: boots the full application against a real MySQL 8.4 started by
 * Testcontainers, with Flyway applying the migrations. Requires a running Docker daemon.
 *
 * <p>Uses the <strong>singleton container</strong> pattern: one MySQL is started once for the whole
 * test JVM and shared across all classes. This matches Spring's cached test context (which resolves
 * the datasource once), avoiding the stale-datasource failures you get when a per-class container is
 * stopped while the cached context lives on.
 *
 * <p>MockMvc is built manually from the {@link WebApplicationContext} with the Spring Security filter
 * chain applied — Spring Boot 4 split the MockMvc test slice into a module not on the classpath, so we
 * avoid {@code @AutoConfigureMockMvc} and wire it here instead.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
                    .withDatabaseName("adiaphora")
                    .withUsername("adiaphora")
                    .withPassword("adiaphora");

    static {
        MYSQL.start();
        Runtime.getRuntime().addShutdownHook(new Thread(MYSQL::stop));
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private CorrelationIdFilter correlationIdFilter;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        // MockMvc does not pick up servlet filters on its own — register the correlation-id filter
        // explicitly so integration tests see the same X-Correlation-Id echo as the real app.
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(correlationIdFilter)
                .apply(springSecurity())
                .build();
    }

    /** Registers a user via the API and returns the created user id. */
    protected String register(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("userId").asText();
    }

    /** Logs in and returns the bearer access token. */
    protected String login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("accessToken").asText();
    }

    protected String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}

