package ru.adiaphora.platform.estate.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for {@link Creditor} aggregates. Implemented in the infrastructure layer. */
public interface CreditorRepository {

    Creditor save(Creditor creditor);

    Optional<Creditor> findById(UUID id);

    List<Creditor> findByApplicationId(UUID applicationId);

    void deleteById(UUID id);
}
