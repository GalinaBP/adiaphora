package ru.adiaphora.platform.questionnaire.api;

import java.util.Optional;
import java.util.UUID;

/** Public access to an application's questionnaire snapshot, for the rules module. */
public interface QuestionnaireQueryService {

    Optional<QuestionnaireSnapshot> snapshot(UUID applicationId);
}
