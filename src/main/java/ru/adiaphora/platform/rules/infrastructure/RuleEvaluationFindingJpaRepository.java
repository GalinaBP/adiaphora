package ru.adiaphora.platform.rules.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface RuleEvaluationFindingJpaRepository extends JpaRepository<RuleEvaluationFindingEntity, UUID> {

    List<RuleEvaluationFindingEntity> findByEvaluationId(UUID evaluationId);
}
