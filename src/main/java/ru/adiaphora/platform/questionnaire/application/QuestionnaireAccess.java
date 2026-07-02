package ru.adiaphora.platform.questionnaire.application;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.application.api.ApplicationQueryService;
import ru.adiaphora.platform.application.api.ApplicationView;
import ru.adiaphora.platform.common.error.AccessDeniedBusinessException;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.common.security.AuthenticatedUser;
import ru.adiaphora.platform.common.security.Authorities;
import ru.adiaphora.platform.common.security.CurrentUser;

import java.util.UUID;

/**
 * Authorization for questionnaire operations on an application: a normal {@code USER} may only touch
 * their own case; staff may touch any. Mirrors the application module's ownership rule, resolved
 * through {@link ApplicationQueryService} so this module never reaches into the application internals.
 */
@Component
class QuestionnaireAccess {

    private final ApplicationQueryService applications;
    private final CurrentUser currentUser;

    QuestionnaireAccess(ApplicationQueryService applications, CurrentUser currentUser) {
        this.applications = applications;
        this.currentUser = currentUser;
    }

    /** Verifies access and returns the application view. */
    ApplicationView requireAccess(UUID applicationId) {
        AuthenticatedUser user = currentUser.require();
        ApplicationView view = applications.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Application", applicationId));
        boolean isUser = Authorities.USER.equals(user.role());
        if (isUser && !view.ownerId().equals(user.userId())) {
            throw new AccessDeniedBusinessException("You may not access this application");
        }
        return view;
    }

    UUID currentUserId() {
        return currentUser.require().userId();
    }
}
