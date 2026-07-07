package ru.adiaphora.platform.estate.application;

/**
 * Result of a create/update: the saved aggregate plus a non-blocking {@code duplicateWarning} flag,
 * set when another entry in the same application looks like a duplicate. Duplicates are warned, not
 * rejected — the user may legitimately have two similar creditors/assets.
 */
public record Saved<T>(T value, boolean duplicateWarning) {
}
