package ru.adiaphora.platform.application.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.application.api.ApplicationCreatedEvent;
import ru.adiaphora.platform.application.domain.BankruptcyApplication;
import ru.adiaphora.platform.application.domain.BankruptcyApplicationRepository;
import ru.adiaphora.platform.common.security.CurrentUser;

import java.time.Clock;
import java.util.UUID;

/** Creates a new bankruptcy case owned by the current user. */
@Service
public class CreateApplicationUseCase {

    private final BankruptcyApplicationRepository repository;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public CreateApplicationUseCase(BankruptcyApplicationRepository repository, CurrentUser currentUser,
                                    ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public UUID create() {
        UUID ownerId = currentUser.require().userId();
        BankruptcyApplication application =
                BankruptcyApplication.create(UUID.randomUUID(), ownerId, clock.instant());
        BankruptcyApplication saved = repository.save(application);
        events.publishEvent(new ApplicationCreatedEvent(saved.id(), ownerId, clock.instant()));
        return saved.id();
    }
}
