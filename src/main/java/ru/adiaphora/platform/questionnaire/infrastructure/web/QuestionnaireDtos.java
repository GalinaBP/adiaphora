package ru.adiaphora.platform.questionnaire.infrastructure.web;

import jakarta.validation.constraints.Size;
import ru.adiaphora.platform.common.error.ApiFieldError;
import ru.adiaphora.platform.questionnaire.application.CompletionSummary;
import ru.adiaphora.platform.questionnaire.application.QuestionnaireForm;
import ru.adiaphora.platform.questionnaire.application.ValidationResult;
import ru.adiaphora.platform.questionnaire.domain.QuestionDefinition;
import ru.adiaphora.platform.questionnaire.domain.QuestionSection;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireDefinition;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Request/response payloads for the questionnaire endpoints. */
public final class QuestionnaireDtos {

    private QuestionnaireDtos() {
    }

    public record SaveAnswerRequest(@Size(max = 4000) String value) {
    }

    public record OptionResponse(String value, String label, int displayOrder) {
    }

    public record QuestionResponse(String code, String sectionCode, String type, String label,
                                   String helpText, boolean required, int displayOrder,
                                   List<OptionResponse> options) {
        static QuestionResponse from(QuestionDefinition q) {
            List<OptionResponse> options = q.options().stream()
                    .map(o -> new OptionResponse(o.value(), o.label(), o.displayOrder()))
                    .toList();
            return new QuestionResponse(q.code(), q.sectionCode(), q.type().name(), q.label(),
                    q.helpText(), q.required(), q.displayOrder(), options);
        }
    }

    public record SectionResponse(String code, String title, int displayOrder) {
        static SectionResponse from(QuestionSection s) {
            return new SectionResponse(s.code(), s.title(), s.displayOrder());
        }
    }

    public record DefinitionResponse(String versionCode, String label, List<SectionResponse> sections,
                                     List<QuestionResponse> questions) {
        public static DefinitionResponse from(QuestionnaireDefinition definition) {
            return new DefinitionResponse(
                    definition.versionCode(),
                    definition.label(),
                    definition.sections().stream().map(SectionResponse::from).toList(),
                    definition.orderedQuestions().stream().map(QuestionResponse::from).toList());
        }
    }

    public record CompletionResponse(int requiredTotal, int requiredAnswered,
                                     List<String> missingRequired, boolean complete) {
        public static CompletionResponse from(CompletionSummary c) {
            return new CompletionResponse(c.requiredTotal(), c.requiredAnswered(), c.missingRequired(),
                    c.complete());
        }
    }

    public record FormResponse(UUID applicationId, String versionCode, String label,
                               List<SectionResponse> sections, List<QuestionResponse> questions,
                               Map<String, String> answers, CompletionResponse completion) {
        public static FormResponse from(QuestionnaireForm form) {
            return new FormResponse(
                    form.applicationId(),
                    form.versionCode(),
                    form.label(),
                    form.sections().stream().map(SectionResponse::from).toList(),
                    form.questions().stream().map(QuestionResponse::from).toList(),
                    form.answers(),
                    CompletionResponse.from(form.completion()));
        }
    }

    public record ValidationResponse(boolean complete, List<String> missingRequired,
                                     List<ApiFieldError> fieldErrors) {
        public static ValidationResponse from(ValidationResult result) {
            return new ValidationResponse(result.complete(), result.missingRequired(),
                    result.fieldErrors());
        }
    }
}
