package ru.adiaphora.platform.estate.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.adiaphora.platform.common.web.ApiPaths;
import ru.adiaphora.platform.estate.application.AssetService;

import java.util.List;
import java.util.UUID;

/** CRUD endpoints for a case's assets. Ownership is enforced in the service layer. */
@Tag(name = "Assets")
@RestController
@RequestMapping(ApiPaths.API_V1 + "/applications/{applicationId}/assets")
class AssetController {

    private final AssetService assets;

    AssetController(AssetService assets) {
        this.assets = assets;
    }

    @Operation(summary = "List assets for a case")
    @GetMapping
    List<EstateDtos.AssetResponse> list(@PathVariable UUID applicationId) {
        return assets.list(applicationId).stream().map(EstateDtos.AssetResponse::of).toList();
    }

    @Operation(summary = "Get an asset")
    @GetMapping("/{assetId}")
    EstateDtos.AssetResponse get(@PathVariable UUID applicationId, @PathVariable UUID assetId) {
        return EstateDtos.AssetResponse.of(assets.get(applicationId, assetId));
    }

    @Operation(summary = "Add an asset (duplicates are flagged, not rejected)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EstateDtos.AssetResponse create(@PathVariable UUID applicationId,
                                    @Valid @RequestBody EstateDtos.AssetRequest request) {
        return EstateDtos.AssetResponse.of(assets.create(applicationId, request.toDetails()));
    }

    @Operation(summary = "Edit an asset")
    @PutMapping("/{assetId}")
    EstateDtos.AssetResponse update(@PathVariable UUID applicationId, @PathVariable UUID assetId,
                                    @Valid @RequestBody EstateDtos.AssetRequest request) {
        return EstateDtos.AssetResponse.of(assets.update(applicationId, assetId, request.toDetails()));
    }

    @Operation(summary = "Delete an asset")
    @DeleteMapping("/{assetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID applicationId, @PathVariable UUID assetId) {
        assets.delete(applicationId, assetId);
    }
}
