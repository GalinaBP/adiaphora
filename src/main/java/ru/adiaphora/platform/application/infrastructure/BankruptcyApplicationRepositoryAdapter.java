package ru.adiaphora.platform.application.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.adiaphora.platform.application.domain.BankruptcyApplication;
import ru.adiaphora.platform.application.domain.BankruptcyApplicationRepository;
import ru.adiaphora.platform.application.domain.StatusChange;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapts Spring Data JPA to the domain {@link BankruptcyApplicationRepository}. On save it upserts the
 * aggregate and appends any accumulated status-history rows drained from the aggregate.
 */
@Component
class BankruptcyApplicationRepositoryAdapter implements BankruptcyApplicationRepository {

    private final BankruptcyApplicationJpaRepository applications;
    private final ApplicationStatusHistoryJpaRepository history;

    BankruptcyApplicationRepositoryAdapter(BankruptcyApplicationJpaRepository applications,
                                           ApplicationStatusHistoryJpaRepository history) {
        this.applications = applications;
        this.history = history;
    }

    @Override
    public BankruptcyApplication save(BankruptcyApplication application) {
        BankruptcyApplicationEntity entity = applications.findById(application.id())
                .map(existing -> {
                    existing.applyFrom(application);
                    return existing;
                })
                .orElseGet(() -> BankruptcyApplicationEntity.fromDomain(application));
        BankruptcyApplicationEntity saved = applications.save(entity);

        List<StatusChange> changes = application.drainNewStatusChanges();
        if (!changes.isEmpty()) {
            history.saveAll(changes.stream()
                    .map(change -> ApplicationStatusHistoryEntity.of(application.id(), change))
                    .toList());
        }
        return saved.toDomain();
    }

    @Override
    public Optional<BankruptcyApplication> findById(UUID applicationId) {
        return applications.findById(applicationId).map(BankruptcyApplicationEntity::toDomain);
    }

    @Override
    public Page<BankruptcyApplication> findByOwnerId(UUID ownerId, Pageable pageable) {
        return applications.findByOwnerId(ownerId, pageable).map(BankruptcyApplicationEntity::toDomain);
    }

    @Override
    public Page<BankruptcyApplication> findAll(Pageable pageable) {
        return applications.findAll(pageable).map(BankruptcyApplicationEntity::toDomain);
    }

    @Override
    public List<StatusChange> findStatusHistory(UUID applicationId) {
        return history.findByApplicationIdOrderByChangedAtAsc(applicationId).stream()
                .map(ApplicationStatusHistoryEntity::toDomain)
                .toList();
    }
}
