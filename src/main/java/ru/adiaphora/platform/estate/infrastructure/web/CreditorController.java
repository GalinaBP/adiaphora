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
import ru.adiaphora.platform.estate.application.CreditorService;

import java.util.List;
import java.util.UUID;

/** CRUD endpoints for a case's creditors. Ownership is enforced in the service layer. */
@Tag(name = "Creditors")
@RestController
@RequestMapping(ApiPaths.API_V1 + "/applications/{applicationId}/creditors")
class CreditorController {

    private final CreditorService creditors;

    CreditorController(CreditorService creditors) {
        this.creditors = creditors;
    }

    @Operation(summary = "List creditors for a case")
    @GetMapping
    List<EstateDtos.CreditorResponse> list(@PathVariable UUID applicationId) {
        return creditors.list(applicationId).stream().map(EstateDtos.CreditorResponse::of).toList();
    }

    @Operation(summary = "Get a creditor")
    @GetMapping("/{creditorId}")
    EstateDtos.CreditorResponse get(@PathVariable UUID applicationId, @PathVariable UUID creditorId) {
        return EstateDtos.CreditorResponse.of(creditors.get(applicationId, creditorId));
    }

    @Operation(summary = "Add a creditor (duplicates are flagged, not rejected)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EstateDtos.CreditorResponse create(@PathVariable UUID applicationId,
                                       @Valid @RequestBody EstateDtos.CreditorRequest request) {
        return EstateDtos.CreditorResponse.of(creditors.create(applicationId, request.toDetails()));
    }

    @Operation(summary = "Edit a creditor")
    @PutMapping("/{creditorId}")
    EstateDtos.CreditorResponse update(@PathVariable UUID applicationId, @PathVariable UUID creditorId,
                                       @Valid @RequestBody EstateDtos.CreditorRequest request) {
        return EstateDtos.CreditorResponse.of(
                creditors.update(applicationId, creditorId, request.toDetails()));
    }

    @Operation(summary = "Delete a creditor")
    @DeleteMapping("/{creditorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID applicationId, @PathVariable UUID creditorId) {
        creditors.delete(applicationId, creditorId);
    }
}
