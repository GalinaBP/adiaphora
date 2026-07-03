package ru.adiaphora.platform.audit.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.adiaphora.platform.application.api.ApplicationCancelledEvent;
import ru.adiaphora.platform.application.api.ApplicationCreatedEvent;
import ru.adiaphora.platform.application.api.ApplicationStatusChangedEvent;
import ru.adiaphora.platform.application.api.ApplicationSubmittedEvent;
import ru.adiaphora.platform.audit.domain.AuditAction;
import ru.adiaphora.platform.audit.domain.AuditEvent;
import ru.adiaphora.platform.audit.domain.AuditResult;
import ru.adiaphora.platform.auth.api.UserLoginFailedEvent;
import ru.adiaphora.platform.auth.api.UserLoginSucceededEvent;
import ru.adiaphora.platform.auth.api.UserRegisteredEvent;
import ru.adiaphora.platform.common.security.CurrentUser;
import ru.adiaphora.platform.questionnaire.api.AnswerUpdatedEvent;
import ru.adiaphora.platform.review.api.ReviewAssignedEvent;
import ru.adiaphora.platform.review.api.ReviewDecisionRecordedEvent;
import ru.adiaphora.platform.rules.api.RulesEvaluatedEvent;

/**
 * Translates domain events published by other modules into immutable audit records. This is the only
 * writer of the audit log — business modules never touch the audit repository directly, they only
 * publish events. Listeners run synchronously on the publishing thread so the request correlation id
 * and current principal are still available; each write commits in its own transaction.
 */
@Component
class AuditEventListeners {

    private final AuditRecorder recorder;
    private final CurrentUser currentUser;

    AuditEventListeners(AuditRecorder recorder, CurrentUser currentUser) {
        this.recorder = recorder;
        this.currentUser = currentUser;
    }

    // --- auth -----------------------------------------------------------------

    @EventListener
    void on(UserRegisteredEvent event) {
        recorder.record(AuditEvent.builder()
                .actor(event.userId(), currentRole())
                .action(AuditAction.USER_REGISTERED)
                .object("User", event.userId()), event.occurredAt());
    }

    @EventListener
    void on(UserLoginSucceededEvent event) {
        recorder.record(AuditEvent.builder()
                .actor(event.userId(), currentRole())
                .action(AuditAction.USER_LOGIN_SUCCEEDED)
                .object("User", event.userId()), event.occurredAt());
    }

    @EventListener
    void on(UserLoginFailedEvent event) {
        // No actor id/email is stored — only that a login attempt failed.
        recorder.record(AuditEvent.builder()
                .action(AuditAction.USER_LOGIN_FAILED)
                .object("User", null)
                .result(AuditResult.FAILURE), event.occurredAt());
    }

    // --- application ----------------------------------------------------------

    @EventListener
    void on(ApplicationCreatedEvent event) {
        recorder.record(AuditEvent.builder()
                .actor(event.ownerId(), currentRole())
                .action(AuditAction.APPLICATION_CREATED)
                .object("Application", event.applicationId())
                .applicationId(event.applicationId()), event.occurredAt());
    }

    @EventListener
    void on(ApplicationSubmittedEvent event) {
        recorder.record(AuditEvent.builder()
                .actor(event.ownerId(), currentRole())
                .action(AuditAction.APPLICATION_SUBMITTED)
                .object("Application", event.applicationId())
                .applicationId(event.applicationId()), event.occurredAt());
    }

    @EventListener
    void on(ApplicationCancelledEvent event) {
        recorder.record(AuditEvent.builder()
                .actor(event.ownerId(), currentRole())
                .action(AuditAction.APPLICATION_CANCELLED)
                .object("Application", event.applicationId())
                .applicationId(event.applicationId()), event.occurredAt());
    }

    @EventListener
    void on(ApplicationStatusChangedEvent event) {
        String metadata = "{\"from\":\"" + event.fromStatus() + "\",\"to\":\"" + event.toStatus() + "\"}";
        recorder.record(AuditEvent.builder()
                .actor(event.actorId(), currentRole())
                .action(AuditAction.APPLICATION_STATUS_CHANGED)
                .object("Application", event.applicationId())
                .applicationId(event.applicationId())
                .metadata(metadata), event.occurredAt());
    }

    // --- rules ----------------------------------------------------------------

    @EventListener
    void on(RulesEvaluatedEvent event) {
        String metadata = "{\"route\":\"" + event.route() + "\",\"manualReviewRequired\":"
                + event.manualReviewRequired() + "}";
        recorder.record(AuditEvent.builder()
                .actor(null, currentRole())
                .action(AuditAction.RULES_EVALUATED)
                .object("Application", event.applicationId())
                .applicationId(event.applicationId())
                .metadata(metadata), event.occurredAt());
    }

    // --- review ---------------------------------------------------------------

    @EventListener
    void on(ReviewAssignedEvent event) {
        recorder.record(AuditEvent.builder()
                .actor(null, currentRole())
                .action(AuditAction.REVIEW_ASSIGNED)
                .object("Review", event.reviewId())
                .applicationId(event.applicationId())
                .metadata("{\"assigneeId\":\"" + event.assigneeId() + "\"}"), event.occurredAt());
    }

    @EventListener
    void on(ReviewDecisionRecordedEvent event) {
        recorder.record(AuditEvent.builder()
                .actor(event.reviewerId(), currentRole())
                .action(AuditAction.REVIEW_DECISION_RECORDED)
                .object("Review", event.reviewId())
                .applicationId(event.applicationId())
                .metadata("{\"decision\":\"" + event.decision() + "\"}"), event.occurredAt());
    }

    // --- questionnaire --------------------------------------------------------

    @EventListener
    void on(AnswerUpdatedEvent event) {
        // Only the question code is recorded, never the answer value.
        String metadata = "{\"questionCode\":\"" + event.questionCode() + "\"}";
        recorder.record(AuditEvent.builder()
                .actor(event.actorId(), currentRole())
                .action(AuditAction.ANSWER_UPDATED)
                .object("QuestionAnswer", null)
                .applicationId(event.applicationId())
                .metadata(metadata), event.occurredAt());
    }

    private String currentRole() {
        return currentUser.get().map(ru.adiaphora.platform.common.security.AuthenticatedUser::role)
                .orElse(null);
    }
}
