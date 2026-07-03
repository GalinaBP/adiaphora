/**
 * Review module: the manual-review workflow — tasks are opened automatically when the rules engine
 * flags an application, then assigned, decided (approve/reject), and can request more information.
 * Route overrides are always documented (reason + reviewer + ruleset version). Consumes
 * {@code rules.api} (trigger event) and {@code application.api} (status/route + query), and
 * {@code auth.api} (assignee lookup); exposes {@code review.api} events for {@code audit}.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Review")
package ru.adiaphora.platform.review;
