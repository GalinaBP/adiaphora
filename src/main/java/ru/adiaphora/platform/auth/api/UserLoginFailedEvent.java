package ru.adiaphora.platform.auth.api;

import ru.adiaphora.platform.common.event.DomainEvent;

import java.time.Instant;

/** Published on a failed login attempt. Consumed by {@code audit}. Never carries the password. */
public record UserLoginFailedEvent(String email, Instant occurredAt) implements DomainEvent {
}
