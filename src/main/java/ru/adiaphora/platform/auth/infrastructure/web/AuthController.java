package ru.adiaphora.platform.auth.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.adiaphora.platform.auth.application.AuthTokens;
import ru.adiaphora.platform.auth.application.AuthenticateUserUseCase;
import ru.adiaphora.platform.auth.application.GetCurrentUserUseCase;
import ru.adiaphora.platform.auth.application.LoginCommand;
import ru.adiaphora.platform.auth.application.LogoutUseCase;
import ru.adiaphora.platform.auth.application.RefreshTokensUseCase;
import ru.adiaphora.platform.auth.application.RegisterCommand;
import ru.adiaphora.platform.auth.application.RegisterUserUseCase;
import ru.adiaphora.platform.common.security.CurrentUser;
import ru.adiaphora.platform.common.web.ApiPaths;

import java.util.UUID;

/** REST endpoints for registration, login, token refresh, logout, and the current-user profile. */
@Tag(name = "Authentication")
@RestController
@RequestMapping(ApiPaths.API_V1 + "/auth")
class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RefreshTokensUseCase refreshTokensUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final CurrentUser currentUser;

    AuthController(RegisterUserUseCase registerUserUseCase,
                   AuthenticateUserUseCase authenticateUserUseCase,
                   RefreshTokensUseCase refreshTokensUseCase,
                   LogoutUseCase logoutUseCase,
                   GetCurrentUserUseCase getCurrentUserUseCase,
                   CurrentUser currentUser) {
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.refreshTokensUseCase = refreshTokensUseCase;
        this.logoutUseCase = logoutUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponses.RegisterResponse register(@Valid @RequestBody AuthRequests.RegisterRequest request) {
        UUID userId = registerUserUseCase.register(new RegisterCommand(request.email(), request.password()));
        return new AuthResponses.RegisterResponse(userId);
    }

    @Operation(summary = "Authenticate and obtain access + refresh tokens")
    @PostMapping("/login")
    AuthResponses.TokenResponse login(@Valid @RequestBody AuthRequests.LoginRequest request) {
        AuthTokens tokens = authenticateUserUseCase.login(new LoginCommand(request.email(), request.password()));
        return AuthResponses.TokenResponse.from(tokens);
    }

    @Operation(summary = "Exchange a refresh token for a new token pair")
    @PostMapping("/refresh")
    AuthResponses.TokenResponse refresh(@Valid @RequestBody AuthRequests.RefreshRequest request) {
        AuthTokens tokens = refreshTokensUseCase.refresh(request.refreshToken());
        return AuthResponses.TokenResponse.from(tokens);
    }

    @Operation(summary = "Log out, revoking outstanding refresh tokens")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout() {
        logoutUseCase.logout(currentUser.require().userId());
    }

    @Operation(summary = "Return the authenticated user's profile")
    @GetMapping("/me")
    AuthResponses.MeResponse me() {
        return AuthResponses.MeResponse.from(
                getCurrentUserUseCase.currentProfile(currentUser.require().userId()));
    }
}
