package ru.adiaphora.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/**
 * Entry point for the Adiaphora bankruptcy document preparation platform.
 *
 * <p>The application is a <strong>modular monolith</strong>: a single Spring Boot deployment split
 * into feature modules ({@code auth}, {@code application}, {@code questionnaire}, {@code rules},
 * {@code review}, {@code document}, {@code audit}) that communicate only through each other's
 * public {@code api} package and Spring application events. Boundaries are verified by
 * Spring Modulith and ArchUnit tests.
 */
@Modulithic(systemName = "Adiaphora Bankruptcy Platform", sharedModules = "common")
@SpringBootApplication
public class AdiaphoraPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdiaphoraPlatformApplication.class, args);
    }
}
