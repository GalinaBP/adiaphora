package ru.adiaphora.platform.auth.application;

/** Application command to register a new self-service user (always assigned the {@code USER} role). */
public record RegisterCommand(String email, String rawPassword) {
}
