package ru.adiaphora.platform.questionnaire.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface QuestionDefinitionJpaRepository extends JpaRepository<QuestionDefinitionEntity, UUID> {

    List<QuestionDefinitionEntity> findByVersionId(UUID versionId);
}
