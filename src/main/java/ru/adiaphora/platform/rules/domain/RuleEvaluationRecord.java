package ru.adiaphora.platform.rules.domain;

import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.questionnaire.api.QuestionnaireSnapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A persisted evaluation outcome for an application: the ruleset/questionnaire versions used, a hash
 * identifying the exact input snapshot, timing, derived route, manual-review flag, the triggered rules
 * (with internal reasons, for operators), and which information was missing.
 *
 * <p>Records are immutable history: a re-evaluation (or a new ruleset release) always produces a new
 * record and never rewrites an existing one.
 */
public record RuleEvaluationRecord(
        UUID id,
        UUID applicationId,
        String rulesetVersion,
        String questionnaireVersion,
        String inputSnapshotHash,
        Instant startedAt,
        Instant completedAt,
        BankruptcyRoute route,
        boolean manualReviewRequired,
        List<RuleEvaluation> triggeredRules,
        List<String> missingInformation
) {

    public static RuleEvaluationRecord create(UUID applicationId, QuestionnaireSnapshot snapshot,
                                              Instant startedAt, Instant completedAt,
                                              EngineResult result) {
        return new RuleEvaluationRecord(
                UUID.randomUUID(),
                applicationId,
                RuleInputs.RULESET_VERSION,
                snapshot.versionCode(),
                hashOf(snapshot),
                startedAt,
                completedAt,
                result.route(),
                result.manualReviewRequired(),
                result.triggered(),
                result.missingInformation());
    }

    /**
     * Deterministic SHA-256 (lowercase hex) over the questionnaire version and the answers sorted by
     * question code. Identifies the exact input a historical evaluation was computed from, so auditors
     * can tell whether the answers have changed since.
     */
    public static String hashOf(QuestionnaireSnapshot snapshot) {
        StringBuilder canonical = new StringBuilder(
                snapshot.versionCode() == null ? "" : snapshot.versionCode());
        snapshot.answers().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> canonical.append('\n')
                        .append(entry.getKey()).append('=').append(entry.getValue()));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
