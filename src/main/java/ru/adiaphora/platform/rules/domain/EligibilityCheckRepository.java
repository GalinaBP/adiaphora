package ru.adiaphora.platform.rules.domain;

/** Persistence port for anonymous eligibility-check sessions (append-only). */
public interface EligibilityCheckRepository {

    void save(EligibilityCheckRecord record);
}
