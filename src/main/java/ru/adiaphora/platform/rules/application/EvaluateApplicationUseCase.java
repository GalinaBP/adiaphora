package ru.adiaphora.platform.rules.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.application.api.ApplicationCommandService;
import ru.adiaphora.platform.application.api.ApplicationView;
import ru.adiaphora.platform.application.api.BankruptcyApplicationStatus;
import ru.adiaphora.platform.questionnaire.api.QuestionnaireQueryService;
import ru.adiaphora.platform.questionnaire.api.QuestionnaireSnapshot;
import ru.adiaphora.platform.rules.api.RulesEvaluationResult;
import ru.adiaphora.platform.rules.api.RulesEvaluationService;

import java.util.Map;
import java.util.UUID;

/**
 * Controller-facing evaluation: checks access, gathers the questionnaire snapshot, runs the rules,
 * records the route, and — when the case is ready for evaluation — advances its status to reflect the
 * outcome (manual review, needs information, or approved for document generation).
 */
@Service
public class EvaluateApplicationUseCase {

    private final RulesAccess access;
    private final RulesEvaluationService rulesEvaluationService;
    private final QuestionnaireQueryService questionnaires;
    private final ApplicationCommandService applicationCommands;

    public EvaluateApplicationUseCase(RulesAccess access, RulesEvaluationService rulesEvaluationService,
                                      QuestionnaireQueryService questionnaires,
                                      ApplicationCommandService applicationCommands) {
        this.access = access;
        this.rulesEvaluationService = rulesEvaluationService;
        this.questionnaires = questionnaires;
        this.applicationCommands = applicationCommands;
    }

    @Transactional
    public RulesEvaluationResult evaluate(UUID applicationId) {
        ApplicationView application = access.requireAccess(applicationId);
        UUID actorId = access.currentUserId();

        QuestionnaireSnapshot snapshot = questionnaires.snapshot(applicationId)
                .orElse(new QuestionnaireSnapshot(applicationId, null, Map.of()));

        RulesEvaluationResult result = rulesEvaluationService.evaluate(applicationId, snapshot);

        applicationCommands.recordRoute(applicationId, result.route(),
                "rules evaluation " + result.rulesetVersion());

        if (application.status() == BankruptcyApplicationStatus.READY_FOR_EVALUATION) {
            applicationCommands.transitionStatus(applicationId, decideTargetStatus(result),
                    "rules evaluation outcome", actorId);
        }
        return result;
    }

    private BankruptcyApplicationStatus decideTargetStatus(RulesEvaluationResult result) {
        if (result.manualReviewRequired()) {
            return BankruptcyApplicationStatus.MANUAL_REVIEW_REQUIRED;
        }
        if (!result.missingInformation().isEmpty()) {
            return BankruptcyApplicationStatus.NEEDS_INFORMATION;
        }
        return BankruptcyApplicationStatus.APPROVED_FOR_DOCUMENT_GENERATION;
    }
}
