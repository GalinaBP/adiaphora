package ru.adiaphora.platform.application.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import ru.adiaphora.platform.application.domain.BankruptcyApplication;
import ru.adiaphora.platform.application.domain.BankruptcyApplicationRepository;
import ru.adiaphora.platform.auth.api.AuthUserView;
import ru.adiaphora.platform.auth.api.UserDirectory;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Seeds a single draft case for the synthetic {@code user@example.test} account (local profile only).
 * Runs after {@code AuthSeedData} so the owner exists. Idempotent: skips if the user already has a case.
 */
@Component
@ConditionalOnProperty(prefix = "adiaphora.seed", name = "enabled", havingValue = "true")
@Order(2)
class ApplicationSeedData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ApplicationSeedData.class);
    private static final String SEED_OWNER_EMAIL = "user@example.test";

    private final BankruptcyApplicationRepository applications;
    private final UserDirectory userDirectory;
    private final Clock clock;

    ApplicationSeedData(BankruptcyApplicationRepository applications, UserDirectory userDirectory,
                        Clock clock) {
        this.applications = applications;
        this.userDirectory = userDirectory;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        Optional<AuthUserView> owner = userDirectory.findByEmail(SEED_OWNER_EMAIL);
        if (owner.isEmpty()) {
            return;
        }
        UUID ownerId = owner.get().userId();
        boolean alreadySeeded = !applications.findByOwnerId(ownerId, PageRequest.of(0, 1))
                .getContent().isEmpty();
        if (alreadySeeded) {
            return;
        }
        BankruptcyApplication draft =
                BankruptcyApplication.create(UUID.randomUUID(), ownerId, clock.instant());
        applications.save(draft);
        log.info("Seeded one draft bankruptcy case for {}", SEED_OWNER_EMAIL);
    }
}
