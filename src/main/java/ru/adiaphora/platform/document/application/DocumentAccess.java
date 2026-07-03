package ru.adiaphora.platform.document.application;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.application.api.ApplicationQueryService;
import ru.adiaphora.platform.application.api.ApplicationView;
import ru.adiaphora.platform.common.error.AccessDeniedBusinessException;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.common.security.AuthenticatedUser;
import ru.adiaphora.platform.common.security.Authorities;
import ru.adiaphora.platform.common.security.CurrentUser;

import java.util.UUID;

/** Ownership check for document operations: a normal USER may only touch their own case's documents. */
@Component
class DocumentAccess {

    private final ApplicationQueryService applications;
    private final CurrentUser currentUser;

    DocumentAccess(ApplicationQueryService applications, CurrentUser currentUser) {
        this.applications = applications;
        this.currentUser = currentUser;
    }

    ApplicationView requireAccess(UUID applicationId) {
        AuthenticatedUser user = currentUser.require();
        ApplicationView view = applications.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Application", applicationId));
        if (Authorities.USER.equals(user.role()) && !view.ownerId().equals(user.userId())) {
            throw new AccessDeniedBusinessException("You may not access this application's documents");
        }
        return view;
    }

    UUID currentUserId() {
        return currentUser.require().userId();
    }
}
