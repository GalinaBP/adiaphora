package ru.adiaphora.platform.application.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.application.api.ApplicationSubmittedEvent;
import ru.adiaphora.platform.application.domain.BankruptcyApplication;
import ru.adiaphora.platform.application.domain.BankruptcyApplicationRepository;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.common.security.AuthenticatedUser;
import ru.adiaphora.platform.common.security.CurrentUser;

import java.time.Clock;
import java.util.UUID;

/** Submits a case for evaluation after verifying ownership/authorization and the status transition. */
@Service
public class SubmitApplicationUseCase {

    private final BankruptcyApplicationRepository repository;
    private final ApplicationAccessPolicy accessPolicy;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public SubmitApplicationUseCase(BankruptcyApplicationRepository repository,
                                    ApplicationAccessPolicy accessPolicy, CurrentUser currentUser,
                                    ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.accessPolicy = accessPolicy;
        this.currentUser = currentUser;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void submit(UUID applicationId) {
        AuthenticatedUser user = currentUser.require();
        BankruptcyApplication application = repository.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Application", applicationId));
        accessPolicy.requireAccess(application, user);

        application.submit(user.userId(), clock.instant());
        repository.save(application);
        events.publishEvent(new ApplicationSubmittedEvent(application.id(), application.ownerId(),
                clock.instant()));
    }
}
