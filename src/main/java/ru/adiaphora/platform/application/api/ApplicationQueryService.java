package ru.adiaphora.platform.application.api;

import java.util.Optional;
import java.util.UUID;

/** Public read access to bankruptcy cases for other modules (e.g. rules, review, document). */
public interface ApplicationQueryService {

    Optional<ApplicationView> findById(UUID applicationId);
}
