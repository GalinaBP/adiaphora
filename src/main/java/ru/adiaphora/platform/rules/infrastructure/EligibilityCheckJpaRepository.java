package ru.adiaphora.platform.rules.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface EligibilityCheckJpaRepository extends JpaRepository<EligibilityCheckEntity, UUID> {
}
