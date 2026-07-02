package ru.adiaphora.platform.questionnaire.application;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireDefinition;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireDefinitionRepository;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireResponse;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireResponseRepository;

import java.util.UUID;

/** Shared helper for resolving the active definition and get-or-creating an application's response. */
@Component
class QuestionnaireResponses {

    private final QuestionnaireDefinitionRepository definitions;
    private final QuestionnaireResponseRepository responses;

    QuestionnaireResponses(QuestionnaireDefinitionRepository definitions,
                           QuestionnaireResponseRepository responses) {
        this.definitions = definitions;
        this.responses = responses;
    }

    QuestionnaireDefinition activeDefinition() {
        return definitions.findActive().orElseThrow(() ->
                new ResourceNotFoundException("No active questionnaire version is configured"));
    }

    /** Returns the application's response, creating one bound to the active version if none exists. */
    QuestionnaireResponse ensureFor(UUID applicationId) {
        return responses.findByApplicationId(applicationId).orElseGet(() -> {
            QuestionnaireDefinition definition = activeDefinition();
            QuestionnaireResponse response = QuestionnaireResponse.start(
                    UUID.randomUUID(), applicationId, definition.versionId(), definition.versionCode());
            return responses.save(response);
        });
    }

    QuestionnaireDefinition definitionFor(QuestionnaireResponse response) {
        return definitions.findByVersionId(response.versionId()).orElseGet(this::activeDefinition);
    }

    QuestionnaireResponse save(QuestionnaireResponse response) {
        return responses.save(response);
    }
}
