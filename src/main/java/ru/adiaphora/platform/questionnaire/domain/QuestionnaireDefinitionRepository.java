package ru.adiaphora.platform.questionnaire.domain;

import java.util.Optional;
import java.util.UUID;

/** Domain port for reading questionnaire definitions (versions + sections + questions + options). */
public interface QuestionnaireDefinitionRepository {

    /** The single currently-active version, if one is published. */
    Optional<QuestionnaireDefinition> findActive();

    Optional<QuestionnaireDefinition> findByVersionId(UUID versionId);
}
