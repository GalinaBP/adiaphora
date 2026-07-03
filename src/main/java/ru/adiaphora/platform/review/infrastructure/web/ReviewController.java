package ru.adiaphora.platform.review.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.adiaphora.platform.common.web.ApiPaths;
import ru.adiaphora.platform.common.web.PageResponse;
import ru.adiaphora.platform.review.application.ReviewQueries;
import ru.adiaphora.platform.review.application.ReviewWorkflowService;

import java.util.UUID;

/**
 * Manual-review endpoints. URL access is limited to staff roles by the security matrix; individual
 * actions are further restricted by role: only ADMIN assigns, and only LAWYER/ADMIN decide. AUDITOR
 * has read-only access (no mutating role), so it cannot modify a review.
 */
@Tag(name = "Reviews")
@RestController
@RequestMapping(ApiPaths.API_V1 + "/reviews")
class ReviewController {

    private final ReviewWorkflowService workflow;
    private final ReviewQueries queries;

    ReviewController(ReviewWorkflowService workflow, ReviewQueries queries) {
        this.workflow = workflow;
        this.queries = queries;
    }

    @Operation(summary = "List reviews")
    @GetMapping
    PageResponse<ReviewDtos.ReviewResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return queries.list(pageable).map(ReviewDtos.ReviewResponse::from);
    }

    @Operation(summary = "Get a review")
    @GetMapping("/{reviewId}")
    ReviewDtos.ReviewResponse get(@PathVariable UUID reviewId) {
        return ReviewDtos.ReviewResponse.from(queries.get(reviewId));
    }

    @Operation(summary = "Assign a review to an operator/lawyer (ADMIN only)")
    @PostMapping("/{reviewId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    ReviewDtos.ReviewResponse assign(@PathVariable UUID reviewId,
                                     @Valid @RequestBody ReviewDtos.AssignReviewRequest request) {
        return ReviewDtos.ReviewResponse.from(workflow.assign(reviewId, request.assigneeId()));
    }

    @Operation(summary = "Request additional information")
    @PostMapping("/{reviewId}/request-information")
    @PreAuthorize("hasAnyRole('OPERATOR','LAWYER','ADMIN')")
    ReviewDtos.ReviewResponse requestInformation(@PathVariable UUID reviewId,
                                                 @Valid @RequestBody ReviewDtos.RequestInformationRequest request) {
        return ReviewDtos.ReviewResponse.from(workflow.requestInformation(reviewId, request.reason()));
    }

    @Operation(summary = "Approve a review (LAWYER/ADMIN)")
    @PostMapping("/{reviewId}/approve")
    @PreAuthorize("hasAnyRole('LAWYER','ADMIN')")
    ReviewDtos.ReviewResponse approve(@PathVariable UUID reviewId,
                                      @Valid @RequestBody ReviewDtos.ApproveReviewRequest request) {
        return ReviewDtos.ReviewResponse.from(
                workflow.approve(reviewId, request.newRoute(), request.reason()));
    }

    @Operation(summary = "Reject a review (LAWYER/ADMIN)")
    @PostMapping("/{reviewId}/reject")
    @PreAuthorize("hasAnyRole('LAWYER','ADMIN')")
    ReviewDtos.ReviewResponse reject(@PathVariable UUID reviewId,
                                     @Valid @RequestBody ReviewDtos.RejectReviewRequest request) {
        return ReviewDtos.ReviewResponse.from(workflow.reject(reviewId, request.reason()));
    }
}
