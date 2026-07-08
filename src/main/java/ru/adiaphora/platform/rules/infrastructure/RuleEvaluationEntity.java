package ru.adiaphora.platform.rules.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.application.api.BankruptcyRoute;

import java.time.Instant;
import java.util.UUID;

/** JPA view of a persisted rule evaluation (its findings are stored in a child table). */
@Entity
@Table(name = "rule_evaluations")
class RuleEvaluationEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "application_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID applicationId;

    @Column(name = "ruleset_version", nullable = false, length = 64)
    private String rulesetVersion;

    @Column(name = "questionnaire_version", length = 64)
    private String questionnaireVersion;

    @Column(name = "input_snapshot_hash", length = 64, updatable = false)
    private String inputSnapshotHash;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "route", nullable = false, length = 48)
    private BankruptcyRoute route;

    @Column(name = "manual_review_required", nullable = false)
    private boolean manualReviewRequired;

    @Column(name = "missing_information", length = 1000)
    private String missingInformation;

    protected RuleEvaluationEntity() {
    }

    RuleEvaluationEntity(UUID id, UUID applicationId, String rulesetVersion, String questionnaireVersion,
                         String inputSnapshotHash, Instant startedAt, Instant completedAt,
                         BankruptcyRoute route, boolean manualReviewRequired, String missingInformation) {
        this.id = id;
        this.applicationId = applicationId;
        this.rulesetVersion = rulesetVersion;
        this.questionnaireVersion = questionnaireVersion;
        this.inputSnapshotHash = inputSnapshotHash;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.route = route;
        this.manualReviewRequired = manualReviewRequired;
        this.missingInformation = missingInformation;
    }

    UUID getId() {
        return id;
    }

    UUID getApplicationId() {
        return applicationId;
    }

    String getRulesetVersion() {
        return rulesetVersion;
    }

    String getQuestionnaireVersion() {
        return questionnaireVersion;
    }

    String getInputSnapshotHash() {
        return inputSnapshotHash;
    }

    Instant getStartedAt() {
        return startedAt;
    }

    Instant getCompletedAt() {
        return completedAt;
    }

    BankruptcyRoute getRoute() {
        return route;
    }

    boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    String getMissingInformation() {
        return missingInformation;
    }
}
