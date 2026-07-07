package ru.adiaphora.platform.estate.application;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.application.api.ApplicationQueryService;
import ru.adiaphora.platform.common.error.AccessDeniedBusinessException;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.common.security.AuthenticatedUser;
import ru.adiaphora.platform.common.security.Authorities;
import ru.adiaphora.platform.common.security.CurrentUser;

import java.util.UUID;

/**
 * Authorization for estate operations on an application: a normal {@code USER} may only touch their
 * own case; staff may touch any. Resolved through {@link ApplicationQueryService} so this module never
 * reaches into the application module's internals. Mirrors {@code QuestionnaireAccess}.
 */
@Component
class EstateAccess {

    private final ApplicationQueryService applications;
    private final CurrentUser currentUser;

    EstateAccess(ApplicationQueryService applications, CurrentUser currentUser) {
        this.applications = applications;
        this.currentUser = currentUser;
    }

    /** Verifies the current user may access the given application, or throws 403/404. */
    void requireAccess(UUID applicationId) {
        AuthenticatedUser user = currentUser.require();
        UUID ownerId = applications.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Application", applicationId))
                .ownerId();
        if (Authorities.USER.equals(user.role()) && !ownerId.equals(user.userId())) {
            throw new AccessDeniedBusinessException("You may not access this application");
        }
    }
}
