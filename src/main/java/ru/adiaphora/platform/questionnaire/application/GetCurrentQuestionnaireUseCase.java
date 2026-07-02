package ru.adiaphora.platform.questionnaire.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireDefinition;

/** Returns the current active questionnaire version (definition only, no application context). */
@Service
public class GetCurrentQuestionnaireUseCase {

    private final QuestionnaireResponses responses;

    public GetCurrentQuestionnaireUseCase(QuestionnaireResponses responses) {
        this.responses = responses;
    }

    @Transactional(readOnly = true)
    public QuestionnaireDefinition current() {
        return responses.activeDefinition();
    }
}
