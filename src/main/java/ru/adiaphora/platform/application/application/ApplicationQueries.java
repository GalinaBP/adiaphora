package ru.adiaphora.platform.application.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.application.api.ApplicationView;
import ru.adiaphora.platform.application.api.ApplicationViewedEvent;
import ru.adiaphora.platform.application.domain.BankruptcyApplication;
import ru.adiaphora.platform.application.domain.BankruptcyApplicationRepository;
import ru.adiaphora.platform.application.domain.StatusChange;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.common.security.AuthenticatedUser;
import ru.adiaphora.platform.common.security.CurrentUser;
import ru.adiaphora.platform.common.web.PageResponse;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Owner-facing read operations. Every access is authorization-checked: a normal user sees only their
 * own cases, staff may list all. Distinct from the module-facing {@code ApplicationQueryService},
 * which carries no user context.
 */
@Service
public class ApplicationQueries {

    private final BankruptcyApplicationRepository repository;
    private final ApplicationAccessPolicy accessPolicy;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ApplicationQueries(BankruptcyApplicationRepository repository,
                              ApplicationAccessPolicy accessPolicy, CurrentUser currentUser,
                              ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.accessPolicy = accessPolicy;
        this.currentUser = currentUser;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ApplicationView getById(UUID applicationId) {
        ApplicationView view = ApplicationMapper.toView(loadAuthorized(applicationId));
        events.publishEvent(new ApplicationViewedEvent(applicationId, currentUser.require().userId(),
                clock.instant()));
        return view;
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationView> list(Pageable pageable) {
        AuthenticatedUser user = currentUser.require();
        Page<BankruptcyApplication> page = accessPolicy.isStaff(user)
                ? repository.findAll(pageable)
                : repository.findByOwnerId(user.userId(), pageable);
        return PageResponse.from(page.map(ApplicationMapper::toView));
    }

    @Transactional(readOnly = true)
    public List<StatusChange> statusHistory(UUID applicationId) {
        loadAuthorized(applicationId);
        return repository.findStatusHistory(applicationId);
    }

    private BankruptcyApplication loadAuthorized(UUID applicationId) {
        AuthenticatedUser user = currentUser.require();
        BankruptcyApplication application = repository.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Application", applicationId));
        accessPolicy.requireAccess(application, user);
        return application;
    }
}
