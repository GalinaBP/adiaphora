package ru.adiaphora.platform.rules.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;

import java.util.UUID;

/** JPA view of a single triggered finding. Stores the internal reason for operators (not exposed via API). */
@Entity
@Table(name = "rule_evaluation_findings")
class RuleEvaluationFindingEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "evaluation_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID evaluationId;

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 32)
    private RuleOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 32)
    private RuleSeverity severity;

    @Column(name = "internal_reason", length = 1000)
    private String internalReason;

    @Column(name = "user_message", length = 1000)
    private String userMessage;

    @Column(name = "blocks_automatic_decision", nullable = false)
    private boolean blocksAutomaticDecision;

    protected RuleEvaluationFindingEntity() {
    }

    static RuleEvaluationFindingEntity of(UUID evaluationId, RuleEvaluation evaluation) {
        RuleEvaluationFindingEntity entity = new RuleEvaluationFindingEntity();
        entity.id = UUID.randomUUID();
        entity.evaluationId = evaluationId;
        entity.ruleCode = evaluation.ruleCode();
        entity.outcome = evaluation.outcome();
        entity.severity = evaluation.severity();
        entity.internalReason = evaluation.internalReason();
        entity.userMessage = evaluation.userMessage();
        entity.blocksAutomaticDecision = evaluation.blocksAutomaticDecision();
        return entity;
    }

    RuleEvaluation toDomain() {
        return new RuleEvaluation(ruleCode, outcome, severity, internalReason, userMessage,
                blocksAutomaticDecision);
    }
}
