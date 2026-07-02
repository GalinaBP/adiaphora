/**
 * Rules module: a deterministic engine that evaluates a questionnaire snapshot against placeholder
 * legal/product rules, producing triggered findings, missing-information flags, a manual-review
 * requirement, and a preliminary route. Consumes {@code questionnaire.api} (snapshot) and
 * {@code application.api} (route/status commands); exposes {@code rules.api} (evaluation service,
 * result, evaluated event for audit).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Rules")
package ru.adiaphora.platform.rules;
