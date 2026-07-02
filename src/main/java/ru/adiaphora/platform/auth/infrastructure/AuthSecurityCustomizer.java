package ru.adiaphora.platform.auth.infrastructure;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import ru.adiaphora.platform.common.security.HttpSecurityCustomizer;

/**
 * Contributes the JWT filter to the shared security chain via the {@code common} extension point.
 * This is how {@code auth} injects authentication without {@code common} depending on {@code auth}.
 */
@Component
class AuthSecurityCustomizer implements HttpSecurityCustomizer {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    AuthSecurityCustomizer(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Override
    public void customize(HttpSecurity http) {
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
