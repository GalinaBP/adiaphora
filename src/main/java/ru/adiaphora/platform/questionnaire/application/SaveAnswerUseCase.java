package ru.adiaphora.platform.questionnaire.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.application.api.ApplicationCommandService;
import ru.adiaphora.platform.application.api.ApplicationView;
import ru.adiaphora.platform.application.api.BankruptcyApplicationStatus;
import ru.adiaphora.platform.questionnaire.api.AnswerUpdatedEvent;
import ru.adiaphora.platform.questionnaire.domain.AnswerValidator;
import ru.adiaphora.platform.questionnaire.domain.QuestionDefinition;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireDefinition;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireResponse;

import java.time.Clock;
import java.util.UUID;

/**
 * Saves a single answer incrementally: validates and normalises the value, upserts it, and — on the
 * first answer to a draft case — moves the case to {@code QUESTIONNAIRE_IN_PROGRESS}. Publishes an
 * {@link AnswerUpdatedEvent} (question code only, never the value) for the audit log.
 */
@Service
public class SaveAnswerUseCase {

    private final QuestionnaireAccess access;
    private final QuestionnaireResponses responses;
    private final AnswerValidator validator;
    private final ApplicationCommandService applicationCommands;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public SaveAnswerUseCase(QuestionnaireAccess access, QuestionnaireResponses responses,
                             AnswerValidator validator, ApplicationCommandService applicationCommands,
                             ApplicationEventPublisher events, Clock clock) {
        this.access = access;
        this.responses = responses;
        this.validator = validator;
        this.applicationCommands = applicationCommands;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public CompletionSummary save(UUID applicationId, String questionCode, String rawValue) {
        ApplicationView application = access.requireAccess(applicationId);
        UUID actorId = access.currentUserId();

        QuestionnaireResponse response = responses.ensureFor(applicationId);
        QuestionnaireDefinition definition = responses.definitionFor(response);
        QuestionDefinition question = definition.requireQuestion(questionCode);

        String normalised = validator.normalise(question, rawValue);
        response.upsertAnswer(questionCode, normalised);
        responses.save(response);

        events.publishEvent(new AnswerUpdatedEvent(applicationId, questionCode, actorId, clock.instant()));

        if (application.status() == BankruptcyApplicationStatus.DRAFT) {
            applicationCommands.transitionStatus(applicationId,
                    BankruptcyApplicationStatus.QUESTIONNAIRE_IN_PROGRESS,
                    "questionnaire started", actorId);
        }

        return CompletionSummary.of(definition, response.answers());
    }
}
