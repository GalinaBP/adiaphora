/**
 * Questionnaire module: versioned questionnaire definitions (sections, questions, options), the
 * per-application response with incrementally saved answers, answer validation, and completion
 * calculation. Exposes {@code questionnaire.api} (answer snapshot + query service for the rules
 * module, and the answer-updated event for audit).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Questionnaire")
package ru.adiaphora.platform.questionnaire;
