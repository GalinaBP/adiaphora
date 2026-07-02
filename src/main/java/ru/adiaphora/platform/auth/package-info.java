/**
 * Auth module: registration, login, password hashing, access/refresh tokens, logout, roles, and the
 * current-authenticated-user lookup. Exposes {@code auth.api} (user directory + login/registration
 * events) to other modules and contributes the JWT filter to the shared security chain.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Auth")
package ru.adiaphora.platform.auth;
