package ru.adiaphora.platform.questionnaire.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface QuestionSectionJpaRepository extends JpaRepository<QuestionSectionEntity, UUID> {

    List<QuestionSectionEntity> findByVersionIdOrderByDisplayOrderAsc(UUID versionId);
}
