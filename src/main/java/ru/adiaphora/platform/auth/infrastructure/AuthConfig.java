package ru.adiaphora.platform.auth.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers auth infrastructure configuration properties. */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
class AuthConfig {
}
