package ru.adiaphora.platform.estate.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.estate.domain.Asset;
import ru.adiaphora.platform.estate.domain.AssetDetails;
import ru.adiaphora.platform.estate.domain.AssetRepository;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for assets, each scoped to an application the caller is allowed to access. Create/update flag
 * likely duplicates (same type + description in the same case) without blocking them.
 */
@Service
public class AssetService {

    private final AssetRepository assets;
    private final EstateAccess access;

    public AssetService(AssetRepository assets, EstateAccess access) {
        this.assets = assets;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<Asset> list(UUID applicationId) {
        access.requireAccess(applicationId);
        return assets.findByApplicationId(applicationId);
    }

    @Transactional(readOnly = true)
    public Asset get(UUID applicationId, UUID assetId) {
        access.requireAccess(applicationId);
        return loadInApplication(applicationId, assetId);
    }

    @Transactional
    public Saved<Asset> create(UUID applicationId, AssetDetails details) {
        access.requireAccess(applicationId);
        boolean duplicate = isDuplicate(applicationId, null, details);
        Asset asset = assets.save(Asset.create(UUID.randomUUID(), applicationId, details));
        return new Saved<>(asset, duplicate);
    }

    @Transactional
    public Saved<Asset> update(UUID applicationId, UUID assetId, AssetDetails details) {
        access.requireAccess(applicationId);
        Asset asset = loadInApplication(applicationId, assetId);
        boolean duplicate = isDuplicate(applicationId, assetId, details);
        asset.update(details);
        return new Saved<>(assets.save(asset), duplicate);
    }

    @Transactional
    public void delete(UUID applicationId, UUID assetId) {
        access.requireAccess(applicationId);
        Asset asset = loadInApplication(applicationId, assetId);
        assets.deleteById(asset.id());
    }

    private Asset loadInApplication(UUID applicationId, UUID assetId) {
        return assets.findById(assetId)
                .filter(a -> a.belongsTo(applicationId))
                .orElseThrow(() -> ResourceNotFoundException.of("Asset", assetId));
    }

    private boolean isDuplicate(UUID applicationId, UUID excludeId, AssetDetails details) {
        String key = details.duplicateKey();
        return assets.findByApplicationId(applicationId).stream()
                .filter(a -> !a.id().equals(excludeId))
                .anyMatch(a -> a.details().duplicateKey().equals(key));
    }
}
