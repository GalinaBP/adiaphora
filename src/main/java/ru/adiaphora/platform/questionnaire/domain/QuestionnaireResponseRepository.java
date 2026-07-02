package ru.adiaphora.platform.questionnaire.domain;

import java.util.Optional;
import java.util.UUID;

/** Domain port for the writable per-application questionnaire response (its saved answers). */
public interface QuestionnaireResponseRepository {

    Optional<QuestionnaireResponse> findByApplicationId(UUID applicationId);

    QuestionnaireResponse save(QuestionnaireResponse response);
}
