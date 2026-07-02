package ru.adiaphora.platform.auth.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.adiaphora.platform.auth.domain.User;
import ru.adiaphora.platform.auth.domain.UserRepository;
import ru.adiaphora.platform.auth.domain.UserRole;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seeds one synthetic account per role for local development. Guarded by {@code adiaphora.seed.enabled}
 * (true only in the local profile) so it never runs in test/production. Uses obviously fake emails and
 * a shared development password — <strong>no real personal data</strong>.
 */
@Component
@ConditionalOnProperty(prefix = "adiaphora.seed", name = "enabled", havingValue = "true")
@Order(1)
class AuthSeedData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AuthSeedData.class);

    /** Development-only password for all seed accounts. */
    static final String SEED_PASSWORD = "Password123!";

    private static final Map<String, UserRole> SEED_USERS = Map.of(
            "user@example.test", UserRole.USER,
            "operator@example.test", UserRole.OPERATOR,
            "lawyer@example.test", UserRole.LAWYER,
            "admin@example.test", UserRole.ADMIN,
            "auditor@example.test", UserRole.AUDITOR);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    AuthSeedData(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> created = SEED_USERS.entrySet().stream()
                .filter(entry -> !users.existsByEmail(entry.getKey()))
                .map(entry -> {
                    User user = User.register(UUID.randomUUID(), entry.getKey(),
                            passwordEncoder.encode(SEED_PASSWORD), entry.getValue());
                    users.save(user);
                    return entry.getKey() + " (" + entry.getValue() + ")";
                })
                .toList();

        if (!created.isEmpty()) {
            log.info("Seeded {} local accounts with password '{}': {}",
                    created.size(), SEED_PASSWORD, created);
        }
    }
}
