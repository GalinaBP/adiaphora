package ru.adiaphora.platform.rules.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.adiaphora.platform.common.web.ApiPaths;
import ru.adiaphora.platform.rules.api.RulesEvaluationResult;
import ru.adiaphora.platform.rules.application.EvaluateApplicationUseCase;
import ru.adiaphora.platform.rules.application.GetLatestEvaluationUseCase;

import java.util.UUID;

/** REST endpoints to evaluate an application and read its latest evaluation. */
@Tag(name = "Rules")
@RestController
@RequestMapping(ApiPaths.API_V1 + "/applications/{applicationId}")
class RulesController {

    private final EvaluateApplicationUseCase evaluate;
    private final GetLatestEvaluationUseCase latest;

    RulesController(EvaluateApplicationUseCase evaluate, GetLatestEvaluationUseCase latest) {
        this.evaluate = evaluate;
        this.latest = latest;
    }

    @Operation(summary = "Evaluate the application against the deterministic rule engine")
    @PostMapping("/evaluate")
    RulesEvaluationResult evaluate(@PathVariable UUID applicationId) {
        return evaluate.evaluate(applicationId);
    }

    @Operation(summary = "Get the latest evaluation result for the application")
    @GetMapping("/evaluations/latest")
    RulesEvaluationResult latest(@PathVariable UUID applicationId) {
        return latest.latest(applicationId);
    }
}
