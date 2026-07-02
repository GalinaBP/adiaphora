package ru.adiaphora.platform.questionnaire.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireDefinition;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireResponse;

import java.util.Map;
import java.util.UUID;

/** Returns an application's questionnaire: definition, saved answers, and completion state. */
@Service
public class GetApplicationQuestionnaireUseCase {

    private final QuestionnaireAccess access;
    private final QuestionnaireResponses responses;

    public GetApplicationQuestionnaireUseCase(QuestionnaireAccess access, QuestionnaireResponses responses) {
        this.access = access;
        this.responses = responses;
    }

    @Transactional
    public QuestionnaireForm get(UUID applicationId) {
        access.requireAccess(applicationId);
        QuestionnaireResponse response = responses.ensureFor(applicationId);
        QuestionnaireDefinition definition = responses.definitionFor(response);
        Map<String, String> answers = response.answers();
        return new QuestionnaireForm(
                applicationId,
                definition.versionCode(),
                definition.label(),
                definition.sections(),
                definition.orderedQuestions(),
                answers,
                CompletionSummary.of(definition, answers));
    }
}
