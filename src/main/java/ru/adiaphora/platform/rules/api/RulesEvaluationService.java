package ru.adiaphora.platform.rules.api;

import ru.adiaphora.platform.questionnaire.api.QuestionnaireSnapshot;

import java.util.UUID;

/**
 * Public entry point to the rule engine. Given an application and a questionnaire snapshot, it
 * evaluates the deterministic rules, persists the result, and returns it. Callers supply the snapshot
 * so evaluation is a pure function of its inputs.
 */
public interface RulesEvaluationService {

    RulesEvaluationResult evaluate(UUID applicationId, QuestionnaireSnapshot questionnaire);
}
