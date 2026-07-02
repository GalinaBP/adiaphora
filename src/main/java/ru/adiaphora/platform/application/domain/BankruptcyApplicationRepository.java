package ru.adiaphora.platform.application.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository for the {@link BankruptcyApplication} aggregate. {@code save} also persists any
 * accumulated status-history entries drained from the aggregate.
 */
public interface BankruptcyApplicationRepository {

    BankruptcyApplication save(BankruptcyApplication application);

    Optional<BankruptcyApplication> findById(UUID applicationId);

    Page<BankruptcyApplication> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<BankruptcyApplication> findAll(Pageable pageable);

    java.util.List<StatusChange> findStatusHistory(UUID applicationId);
}
