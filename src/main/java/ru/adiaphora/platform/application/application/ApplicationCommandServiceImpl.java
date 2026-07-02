package ru.adiaphora.platform.application.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.application.api.ApplicationCommandService;
import ru.adiaphora.platform.application.api.ApplicationStatusChangedEvent;
import ru.adiaphora.platform.application.api.BankruptcyApplicationStatus;
import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.application.domain.BankruptcyApplication;
import ru.adiaphora.platform.application.domain.BankruptcyApplicationRepository;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;

import java.time.Clock;
import java.util.UUID;

/**
 * Module-facing implementation of {@link ApplicationCommandService}. Used by {@code rules} and
 * {@code review} to advance the lifecycle; still validates every transition through the aggregate.
 */
@Service
class ApplicationCommandServiceImpl implements ApplicationCommandService {

    private final BankruptcyApplicationRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ApplicationCommandServiceImpl(BankruptcyApplicationRepository repository,
                                  ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void recordRoute(UUID applicationId, BankruptcyRoute route, String reason) {
        BankruptcyApplication application = load(applicationId);
        application.assignRoute(route);
        repository.save(application);
    }

    @Override
    @Transactional
    public void transitionStatus(UUID applicationId, BankruptcyApplicationStatus target, String reason,
                                 UUID actorId) {
        BankruptcyApplication application = load(applicationId);
        BankruptcyApplicationStatus from = application.status();
        application.transitionTo(target, reason, actorId, clock.instant());
        repository.save(application);
        events.publishEvent(new ApplicationStatusChangedEvent(applicationId, from, target, actorId,
                clock.instant()));
    }

    private BankruptcyApplication load(UUID applicationId) {
        return repository.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Application", applicationId));
    }
}
