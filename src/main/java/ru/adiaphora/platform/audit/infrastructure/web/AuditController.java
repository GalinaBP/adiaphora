package ru.adiaphora.platform.audit.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.adiaphora.platform.audit.application.AuditQueries;
import ru.adiaphora.platform.common.web.ApiPaths;
import ru.adiaphora.platform.common.web.PageResponse;

import java.util.UUID;

/**
 * Read-only audit endpoints. Access is restricted to ADMIN/AUDITOR by the security URL matrix; there
 * are deliberately no write endpoints, so the trail is immutable through the API.
 */
@Tag(name = "Audit")
@RestController
@RequestMapping(ApiPaths.API_V1 + "/audit")
class AuditController {

    private final AuditQueries auditQueries;

    AuditController(AuditQueries auditQueries) {
        this.auditQueries = auditQueries;
    }

    @Operation(summary = "List audit events, optionally filtered by application")
    @GetMapping("/events")
    PageResponse<AuditDtos.AuditEventResponse> list(
            @RequestParam(required = false) UUID applicationId,
            @PageableDefault(size = 50) Pageable pageable) {
        return auditQueries.list(applicationId, pageable).map(AuditDtos.AuditEventResponse::from);
    }

    @Operation(summary = "Get a single audit event")
    @GetMapping("/events/{id}")
    AuditDtos.AuditEventResponse get(@PathVariable UUID id) {
        return AuditDtos.AuditEventResponse.from(auditQueries.getById(id));
    }
}
