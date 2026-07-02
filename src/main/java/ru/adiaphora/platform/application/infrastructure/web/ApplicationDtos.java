package ru.adiaphora.platform.application.infrastructure.web;

import jakarta.validation.constraints.Size;
import ru.adiaphora.platform.application.api.ApplicationView;
import ru.adiaphora.platform.application.domain.StatusChange;

import java.time.Instant;
import java.util.UUID;

/** Request/response payloads for the application (bankruptcy case) endpoints. */
public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    public record CreateApplicationResponse(UUID applicationId) {
    }

    public record CancelApplicationRequest(@Size(max = 500) String reason) {
    }

    public record ApplicationResponse(
            UUID applicationId,
            UUID ownerId,
            String status,
            String route,
            Instant submittedAt) {

        public static ApplicationResponse from(ApplicationView view) {
            return new ApplicationResponse(
                    view.applicationId(),
                    view.ownerId(),
                    view.status().name(),
                    view.route().name(),
                    view.submittedAt());
        }
    }

    public record StatusHistoryEntry(
            String fromStatus,
            String toStatus,
            String reason,
            UUID actorId,
            Instant changedAt) {

        public static StatusHistoryEntry from(StatusChange change) {
            return new StatusHistoryEntry(
                    change.from() == null ? null : change.from().name(),
                    change.to().name(),
                    change.reason(),
                    change.actorId(),
                    change.changedAt());
        }
    }
}
