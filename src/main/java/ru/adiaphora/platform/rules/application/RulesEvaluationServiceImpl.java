package ru.adiaphora.platform.rules.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.questionnaire.api.QuestionnaireSnapshot;
import ru.adiaphora.platform.rules.api.RulesEvaluatedEvent;
import ru.adiaphora.platform.rules.api.RulesEvaluationResult;
import ru.adiaphora.platform.rules.api.RulesEvaluationService;
import ru.adiaphora.platform.rules.domain.EngineResult;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEngine;
import ru.adiaphora.platform.rules.domain.RuleEvaluationRecord;
import ru.adiaphora.platform.rules.domain.RuleEvaluationRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Public rules evaluation: runs the engine over the supplied snapshot, persists the result, publishes
 * a {@link RulesEvaluatedEvent}, and returns the public result. Does not change the application
 * lifecycle — that orchestration lives in {@code EvaluateApplicationUseCase}.
 */
@Service
public class RulesEvaluationServiceImpl implements RulesEvaluationService {

    private final RuleEngine engine;
    private final RuleEvaluationRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RulesEvaluationServiceImpl(RuleEngine engine, RuleEvaluationRepository repository,
                                      ApplicationEventPublisher events, Clock clock) {
        this.engine = engine;
        this.repository = repository;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RulesEvaluationResult evaluate(UUID applicationId, QuestionnaireSnapshot questionnaire) {
        Instant startedAt = clock.instant();
        EngineResult engineResult = engine.evaluate(new RuleContext(questionnaire));
        Instant completedAt = clock.instant();

        RuleEvaluationRecord record = repository.save(RuleEvaluationRecord.create(
                applicationId, questionnaire, startedAt, completedAt, engineResult));

        events.publishEvent(new RulesEvaluatedEvent(applicationId, record.rulesetVersion(),
                record.route(), record.manualReviewRequired(), completedAt));

        return RulesResultMapper.toResult(record);
    }
}
