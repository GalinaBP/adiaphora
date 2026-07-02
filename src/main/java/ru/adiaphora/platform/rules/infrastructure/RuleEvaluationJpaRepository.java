package ru.adiaphora.platform.rules.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface RuleEvaluationJpaRepository extends JpaRepository<RuleEvaluationEntity, UUID> {

    Optional<RuleEvaluationEntity> findFirstByApplicationIdOrderByCompletedAtDesc(UUID applicationId);
}
