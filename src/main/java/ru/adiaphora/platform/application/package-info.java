/**
 * Application module: the customer's bankruptcy case lifecycle — creation, ownership, status
 * transitions and history, submission and cancellation, and the preliminary route. Exposes
 * {@code application.api} (views, query/command services, lifecycle events) to other modules.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Application")
package ru.adiaphora.platform.application;
