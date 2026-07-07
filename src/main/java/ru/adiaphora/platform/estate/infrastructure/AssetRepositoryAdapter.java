package ru.adiaphora.platform.estate.infrastructure;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.estate.domain.Asset;
import ru.adiaphora.platform.estate.domain.AssetRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapts Spring Data JPA to the domain {@link AssetRepository}. Upserts on save. */
@Component
class AssetRepositoryAdapter implements AssetRepository {

    private final AssetJpaRepository jpa;

    AssetRepositoryAdapter(AssetJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Asset save(Asset asset) {
        AssetEntity entity = jpa.findById(asset.id())
                .map(existing -> {
                    existing.applyFrom(asset);
                    return existing;
                })
                .orElseGet(() -> AssetEntity.fromDomain(asset));
        return jpa.save(entity).toDomain();
    }

    @Override
    public Optional<Asset> findById(UUID id) {
        return jpa.findById(id).map(AssetEntity::toDomain);
    }

    @Override
    public List<Asset> findByApplicationId(UUID applicationId) {
        return jpa.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream()
                .map(AssetEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}
