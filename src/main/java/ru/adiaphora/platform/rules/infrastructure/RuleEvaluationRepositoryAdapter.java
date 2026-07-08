package ru.adiaphora.platform.rules.infrastructure;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleEvaluationRecord;
import ru.adiaphora.platform.rules.domain.RuleEvaluationRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapts Spring Data JPA to the {@link RuleEvaluationRepository} port. */
@Component
class RuleEvaluationRepositoryAdapter implements RuleEvaluationRepository {

    private final RuleEvaluationJpaRepository evaluations;
    private final RuleEvaluationFindingJpaRepository findings;

    RuleEvaluationRepositoryAdapter(RuleEvaluationJpaRepository evaluations,
                                    RuleEvaluationFindingJpaRepository findings) {
        this.evaluations = evaluations;
        this.findings = findings;
    }

    @Override
    public RuleEvaluationRecord save(RuleEvaluationRecord record) {
        evaluations.save(new RuleEvaluationEntity(
                record.id(), record.applicationId(), record.rulesetVersion(),
                record.questionnaireVersion(), record.inputSnapshotHash(),
                record.startedAt(), record.completedAt(),
                record.route(), record.manualReviewRequired(),
                joinMissing(record.missingInformation())));

        findings.saveAll(record.triggeredRules().stream()
                .map(evaluation -> RuleEvaluationFindingEntity.of(record.id(), evaluation))
                .toList());
        return record;
    }

    @Override
    public Optional<RuleEvaluationRecord> findLatestByApplicationId(UUID applicationId) {
        return evaluations.findFirstByApplicationIdOrderByCompletedAtDesc(applicationId)
                .map(this::toDomain);
    }

    private RuleEvaluationRecord toDomain(RuleEvaluationEntity entity) {
        List<RuleEvaluation> triggered = findings.findByEvaluationId(entity.getId()).stream()
                .map(RuleEvaluationFindingEntity::toDomain)
                .toList();
        return new RuleEvaluationRecord(
                entity.getId(), entity.getApplicationId(), entity.getRulesetVersion(),
                entity.getQuestionnaireVersion(), entity.getInputSnapshotHash(),
                entity.getStartedAt(), entity.getCompletedAt(),
                entity.getRoute(), entity.isManualReviewRequired(), triggered,
                splitMissing(entity.getMissingInformation()));
    }

    private String joinMissing(List<String> missing) {
        return missing.isEmpty() ? null : String.join(",", missing);
    }

    private List<String> splitMissing(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
