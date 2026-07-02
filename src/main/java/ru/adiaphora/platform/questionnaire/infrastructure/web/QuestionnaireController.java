package ru.adiaphora.platform.questionnaire.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.adiaphora.platform.common.web.ApiPaths;
import ru.adiaphora.platform.questionnaire.application.GetApplicationQuestionnaireUseCase;
import ru.adiaphora.platform.questionnaire.application.GetCurrentQuestionnaireUseCase;
import ru.adiaphora.platform.questionnaire.application.SaveAnswerUseCase;
import ru.adiaphora.platform.questionnaire.application.ValidateQuestionnaireUseCase;

import java.util.UUID;

/** REST endpoints for the questionnaire: the current definition, an application's form, saving
 * answers incrementally, and validating the whole questionnaire. */
@Tag(name = "Questionnaire")
@RestController
class QuestionnaireController {

    private final GetCurrentQuestionnaireUseCase getCurrent;
    private final GetApplicationQuestionnaireUseCase getForApplication;
    private final SaveAnswerUseCase saveAnswer;
    private final ValidateQuestionnaireUseCase validate;

    QuestionnaireController(GetCurrentQuestionnaireUseCase getCurrent,
                            GetApplicationQuestionnaireUseCase getForApplication,
                            SaveAnswerUseCase saveAnswer,
                            ValidateQuestionnaireUseCase validate) {
        this.getCurrent = getCurrent;
        this.getForApplication = getForApplication;
        this.saveAnswer = saveAnswer;
        this.validate = validate;
    }

    @Operation(summary = "Get the current active questionnaire definition")
    @GetMapping(ApiPaths.API_V1 + "/questionnaires/current")
    QuestionnaireDtos.DefinitionResponse current() {
        return QuestionnaireDtos.DefinitionResponse.from(getCurrent.current());
    }

    @Operation(summary = "Get an application's questionnaire with saved answers and completion")
    @GetMapping(ApiPaths.API_V1 + "/applications/{applicationId}/questionnaire")
    QuestionnaireDtos.FormResponse forApplication(@PathVariable UUID applicationId) {
        return QuestionnaireDtos.FormResponse.from(getForApplication.get(applicationId));
    }

    @Operation(summary = "Save a single answer incrementally")
    @PutMapping(ApiPaths.API_V1 + "/applications/{applicationId}/answers/{questionCode}")
    QuestionnaireDtos.CompletionResponse saveAnswer(@PathVariable UUID applicationId,
                                                    @PathVariable String questionCode,
                                                    @Valid @RequestBody QuestionnaireDtos.SaveAnswerRequest request) {
        return QuestionnaireDtos.CompletionResponse.from(
                saveAnswer.save(applicationId, questionCode, request.value()));
    }

    @Operation(summary = "Validate the whole questionnaire")
    @PostMapping(ApiPaths.API_V1 + "/applications/{applicationId}/questionnaire/validate")
    QuestionnaireDtos.ValidationResponse validate(@PathVariable UUID applicationId) {
        return QuestionnaireDtos.ValidationResponse.from(validate.validate(applicationId));
    }
}
