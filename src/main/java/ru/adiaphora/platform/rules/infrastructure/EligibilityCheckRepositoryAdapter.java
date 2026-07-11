package ru.adiaphora.platform.rules.infrastructure;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.rules.domain.EligibilityCheckRecord;
import ru.adiaphora.platform.rules.domain.EligibilityCheckRepository;

/** Adapts Spring Data JPA to the {@link EligibilityCheckRepository} port. */
@Component
class EligibilityCheckRepositoryAdapter implements EligibilityCheckRepository {

    private final EligibilityCheckJpaRepository checks;

    EligibilityCheckRepositoryAdapter(EligibilityCheckJpaRepository checks) {
        this.checks = checks;
    }

    @Override
    public void save(EligibilityCheckRecord record) {
        checks.save(EligibilityCheckEntity.of(record));
    }
}
