package ru.adiaphora.platform.auth.api;

import ru.adiaphora.platform.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when a new user registers. Consumed by {@code audit}. Carries no credential material. */
public record UserRegisteredEvent(UUID userId, String email, Instant occurredAt) implements DomainEvent {
}
