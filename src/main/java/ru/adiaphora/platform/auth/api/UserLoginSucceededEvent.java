package ru.adiaphora.platform.auth.api;

import ru.adiaphora.platform.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Published on a successful login. Consumed by {@code audit}. */
public record UserLoginSucceededEvent(UUID userId, Instant occurredAt) implements DomainEvent {
}
