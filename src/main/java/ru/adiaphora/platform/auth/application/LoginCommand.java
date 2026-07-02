package ru.adiaphora.platform.auth.application;

/** Application command to authenticate with email + raw password. */
public record LoginCommand(String email, String rawPassword) {
}
