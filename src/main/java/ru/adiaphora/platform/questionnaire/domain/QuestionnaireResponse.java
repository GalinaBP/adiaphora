package ru.adiaphora.platform.questionnaire.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The per-application answer set for a given questionnaire version. Answers are stored as normalised
 * strings keyed by question code and are updated incrementally (one at a time).
 */
public class QuestionnaireResponse {

    private final UUID id;
    private final UUID applicationId;
    private final UUID versionId;
    private final String versionCode;
    private final Map<String, String> answers;

    private QuestionnaireResponse(UUID id, UUID applicationId, UUID versionId, String versionCode,
                                  Map<String, String> answers) {
        this.id = id;
        this.applicationId = applicationId;
        this.versionId = versionId;
        this.versionCode = versionCode;
        this.answers = new HashMap<>(answers);
    }

    public static QuestionnaireResponse start(UUID id, UUID applicationId, UUID versionId,
                                              String versionCode) {
        return new QuestionnaireResponse(id, applicationId, versionId, versionCode, Map.of());
    }

    public static QuestionnaireResponse rehydrate(UUID id, UUID applicationId, UUID versionId,
                                                  String versionCode, Map<String, String> answers) {
        return new QuestionnaireResponse(id, applicationId, versionId, versionCode, answers);
    }

    /** Inserts or replaces the answer for a question. The value is assumed already validated/normalised. */
    public void upsertAnswer(String questionCode, String normalisedValue) {
        answers.put(questionCode, normalisedValue);
    }

    public Map<String, String> answers() {
        return Map.copyOf(answers);
    }

    public UUID id() {
        return id;
    }

    public UUID applicationId() {
        return applicationId;
    }

    public UUID versionId() {
        return versionId;
    }

    public String versionCode() {
        return versionCode;
    }
}
