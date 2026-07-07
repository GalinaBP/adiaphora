package ru.adiaphora.platform.estate.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for {@link Asset} aggregates. Implemented in the infrastructure layer. */
public interface AssetRepository {

    Asset save(Asset asset);

    Optional<Asset> findById(UUID id);

    List<Asset> findByApplicationId(UUID applicationId);

    void deleteById(UUID id);
}
