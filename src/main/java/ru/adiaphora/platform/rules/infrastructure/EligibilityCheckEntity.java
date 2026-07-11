package ru.adiaphora.platform.rules.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.rules.domain.EligibilityCheckRecord;

import java.time.Instant;
import java.util.UUID;

/** JPA view of a persisted anonymous eligibility-check session (append-only, no personal data). */
@Entity
@Table(name = "eligibility_checks")
class EligibilityCheckEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "answers_json", nullable = false, length = 4000, updatable = false)
    private String answersJson;

    @Column(name = "verdict", nullable = false, length = 32, updatable = false)
    private String verdict;

    @Column(name = "route", nullable = false, length = 48, updatable = false)
    private String route;

    @Column(name = "qualifying_grounds", length = 200, updatable = false)
    private String qualifyingGrounds;

    @Column(name = "citations", length = 500, updatable = false)
    private String citations;

    @Column(name = "ruleset_version", nullable = false, length = 64, updatable = false)
    private String rulesetVersion;

    protected EligibilityCheckEntity() {
    }

    static EligibilityCheckEntity of(EligibilityCheckRecord record) {
        EligibilityCheckEntity entity = new EligibilityCheckEntity();
        entity.id = record.id();
        entity.createdAt = record.createdAt();
        entity.answersJson = record.answersJson();
        entity.verdict = record.verdict();
        entity.route = record.route();
        entity.qualifyingGrounds = record.qualifyingGrounds();
        entity.citations = record.citations();
        entity.rulesetVersion = record.rulesetVersion();
        return entity;
    }
}
