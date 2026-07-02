package ru.adiaphora.platform.rules.application;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.application.api.ApplicationQueryService;
import ru.adiaphora.platform.application.api.ApplicationView;
import ru.adiaphora.platform.common.error.AccessDeniedBusinessException;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.common.security.AuthenticatedUser;
import ru.adiaphora.platform.common.security.Authorities;
import ru.adiaphora.platform.common.security.CurrentUser;

import java.util.UUID;

/** Ownership check for rules operations: a normal USER may only evaluate their own case; staff any. */
@Component
class RulesAccess {

    private final ApplicationQueryService applications;
    private final CurrentUser currentUser;

    RulesAccess(ApplicationQueryService applications, CurrentUser currentUser) {
        this.applications = applications;
        this.currentUser = currentUser;
    }

    ApplicationView requireAccess(UUID applicationId) {
        AuthenticatedUser user = currentUser.require();
        ApplicationView view = applications.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Application", applicationId));
        if (Authorities.USER.equals(user.role()) && !view.ownerId().equals(user.userId())) {
            throw new AccessDeniedBusinessException("You may not access this application");
        }
        return view;
    }

    UUID currentUserId() {
        return currentUser.require().userId();
    }
}
