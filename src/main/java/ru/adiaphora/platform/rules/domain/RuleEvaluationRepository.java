package ru.adiaphora.platform.rules.domain;

import java.util.Optional;
import java.util.UUID;

/** Domain port for persisting and reading rule-evaluation results. */
public interface RuleEvaluationRepository {

    RuleEvaluationRecord save(RuleEvaluationRecord record);

    Optional<RuleEvaluationRecord> findLatestByApplicationId(UUID applicationId);
}
