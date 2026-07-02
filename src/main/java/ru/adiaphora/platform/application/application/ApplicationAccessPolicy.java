package ru.adiaphora.platform.application.application;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.application.domain.BankruptcyApplication;
import ru.adiaphora.platform.common.error.AccessDeniedBusinessException;
import ru.adiaphora.platform.common.security.AuthenticatedUser;
import ru.adiaphora.platform.common.security.Authorities;

/**
 * Enforces the rule that a normal {@code USER} may act only on their own cases, while staff roles
 * (operator, lawyer, admin, auditor) may access any case. This is the ownership check the ticket
 * requires <em>in addition</em> to the URL-level security rules.
 */
@Component
public class ApplicationAccessPolicy {

    private static final String USER_AUTHORITY = Authorities.USER;

    public boolean canAccess(BankruptcyApplication application, AuthenticatedUser user) {
        if (USER_AUTHORITY.equals(user.role())) {
            return application.isOwnedBy(user.userId());
        }
        return true;
    }

    public void requireAccess(BankruptcyApplication application, AuthenticatedUser user) {
        if (!canAccess(application, user)) {
            throw new AccessDeniedBusinessException("You may not access this application");
        }
    }

    public boolean isStaff(AuthenticatedUser user) {
        return !USER_AUTHORITY.equals(user.role());
    }
}
