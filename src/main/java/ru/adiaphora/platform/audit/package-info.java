/**
 * Audit module: an immutable, event-driven audit log. It subscribes to domain events published by
 * other modules and is the sole writer of the audit trail — business modules never write to it
 * directly. Exposes read-only endpoints to ADMIN/AUDITOR only. Has no public {@code api} package
 * because no module depends on audit; it only consumes their events.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Audit")
package ru.adiaphora.platform.audit;
