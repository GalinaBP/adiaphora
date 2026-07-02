package ru.adiaphora.platform.application.application;

import ru.adiaphora.platform.application.api.ApplicationView;
import ru.adiaphora.platform.application.domain.BankruptcyApplication;

/** Maps the {@link BankruptcyApplication} aggregate to the public {@link ApplicationView} DTO. */
final class ApplicationMapper {

    private ApplicationMapper() {
    }

    static ApplicationView toView(BankruptcyApplication application) {
        return new ApplicationView(
                application.id(),
                application.ownerId(),
                application.status(),
                application.route(),
                application.submittedAt());
    }
}
