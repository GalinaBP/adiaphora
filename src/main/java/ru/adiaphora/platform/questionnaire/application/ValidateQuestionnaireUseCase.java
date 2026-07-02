package ru.adiaphora.platform.questionnaire.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.common.error.ApiFieldError;
import ru.adiaphora.platform.questionnaire.domain.AnswerValidator;
import ru.adiaphora.platform.questionnaire.domain.QuestionDefinition;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireDefinition;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Validates a whole questionnaire: reports per-field format errors among the provided answers and
 * which required questions are still missing. Read-only — it does not change the case status.
 */
@Service
public class ValidateQuestionnaireUseCase {

    private final QuestionnaireAccess access;
    private final QuestionnaireResponses responses;
    private final AnswerValidator validator;

    public ValidateQuestionnaireUseCase(QuestionnaireAccess access, QuestionnaireResponses responses,
                                        AnswerValidator validator) {
        this.access = access;
        this.responses = responses;
        this.validator = validator;
    }

    @Transactional
    public ValidationResult validate(UUID applicationId) {
        access.requireAccess(applicationId);
        QuestionnaireResponse response = responses.ensureFor(applicationId);
        QuestionnaireDefinition definition = responses.definitionFor(response);
        Map<String, String> answers = response.answers();

        List<ApiFieldError> fieldErrors = new ArrayList<>();
        for (QuestionDefinition question : definition.questions()) {
            validator.validate(question, answers.get(question.code()))
                    .ifPresent(message -> fieldErrors.add(new ApiFieldError(question.code(), message)));
        }

        CompletionSummary completion = CompletionSummary.of(definition, answers);
        boolean complete = completion.complete() && fieldErrors.isEmpty();
        return new ValidationResult(complete, completion.missingRequired(), fieldErrors);
    }
}
