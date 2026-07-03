package ru.adiaphora.platform.review.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.adiaphora.platform.application.api.BankruptcyRoute;
import ru.adiaphora.platform.review.application.ReviewView;

import java.util.UUID;

/** Request/response payloads for the review endpoints. */
public final class ReviewDtos {

    private ReviewDtos() {
    }

    public record AssignReviewRequest(@NotNull UUID assigneeId) {
    }

    public record RequestInformationRequest(@NotBlank @Size(max = 1000) String reason) {
    }

    public record ApproveReviewRequest(BankruptcyRoute newRoute, @Size(max = 1000) String reason) {
    }

    public record RejectReviewRequest(@NotBlank @Size(max = 1000) String reason) {
    }

    public record ReviewResponse(
            UUID reviewId,
            UUID applicationId,
            String status,
            UUID assigneeId,
            String route,
            String rulesetVersion,
            String lastDecisionReason) {

        public static ReviewResponse from(ReviewView view) {
            return new ReviewResponse(
                    view.reviewId(),
                    view.applicationId(),
                    view.status().name(),
                    view.assigneeId(),
                    view.route().name(),
                    view.rulesetVersion(),
                    view.lastDecisionReason());
        }
    }
}
