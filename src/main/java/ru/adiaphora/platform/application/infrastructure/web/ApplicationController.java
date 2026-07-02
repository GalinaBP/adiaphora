package ru.adiaphora.platform.application.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.adiaphora.platform.application.application.ApplicationQueries;
import ru.adiaphora.platform.application.application.CancelApplicationUseCase;
import ru.adiaphora.platform.application.application.CreateApplicationUseCase;
import ru.adiaphora.platform.application.application.SubmitApplicationUseCase;
import ru.adiaphora.platform.common.web.ApiPaths;
import ru.adiaphora.platform.common.web.PageResponse;

import java.util.List;
import java.util.UUID;

/** REST endpoints for managing a customer's bankruptcy case (the "application"). */
@Tag(name = "Applications")
@RestController
@RequestMapping(ApiPaths.API_V1 + "/applications")
class ApplicationController {

    private final CreateApplicationUseCase createApplicationUseCase;
    private final SubmitApplicationUseCase submitApplicationUseCase;
    private final CancelApplicationUseCase cancelApplicationUseCase;
    private final ApplicationQueries applicationQueries;

    ApplicationController(CreateApplicationUseCase createApplicationUseCase,
                         SubmitApplicationUseCase submitApplicationUseCase,
                         CancelApplicationUseCase cancelApplicationUseCase,
                         ApplicationQueries applicationQueries) {
        this.createApplicationUseCase = createApplicationUseCase;
        this.submitApplicationUseCase = submitApplicationUseCase;
        this.cancelApplicationUseCase = cancelApplicationUseCase;
        this.applicationQueries = applicationQueries;
    }

    @Operation(summary = "Create a new bankruptcy case for the current user")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApplicationDtos.CreateApplicationResponse create() {
        UUID id = createApplicationUseCase.create();
        return new ApplicationDtos.CreateApplicationResponse(id);
    }

    @Operation(summary = "List cases visible to the current user")
    @GetMapping
    PageResponse<ApplicationDtos.ApplicationResponse> list(Pageable pageable) {
        return applicationQueries.list(pageable).map(ApplicationDtos.ApplicationResponse::from);
    }

    @Operation(summary = "Get a single case")
    @GetMapping("/{applicationId}")
    ApplicationDtos.ApplicationResponse get(@PathVariable UUID applicationId) {
        return ApplicationDtos.ApplicationResponse.from(applicationQueries.getById(applicationId));
    }

    @Operation(summary = "Submit a case for evaluation")
    @PostMapping("/{applicationId}/submit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void submit(@PathVariable UUID applicationId) {
        submitApplicationUseCase.submit(applicationId);
    }

    @Operation(summary = "Cancel a case")
    @PostMapping("/{applicationId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable UUID applicationId,
                @Valid @RequestBody(required = false) ApplicationDtos.CancelApplicationRequest request) {
        String reason = request == null ? null : request.reason();
        cancelApplicationUseCase.cancel(applicationId, reason);
    }

    @Operation(summary = "Get the status history of a case")
    @GetMapping("/{applicationId}/status-history")
    List<ApplicationDtos.StatusHistoryEntry> statusHistory(@PathVariable UUID applicationId) {
        return applicationQueries.statusHistory(applicationId).stream()
                .map(ApplicationDtos.StatusHistoryEntry::from)
                .toList();
    }
}
