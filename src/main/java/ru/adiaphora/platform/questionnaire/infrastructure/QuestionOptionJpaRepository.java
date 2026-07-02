package ru.adiaphora.platform.questionnaire.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface QuestionOptionJpaRepository extends JpaRepository<QuestionOptionEntity, UUID> {

    List<QuestionOptionEntity> findByQuestionDefinitionIdInOrderByDisplayOrderAsc(Collection<UUID> ids);
}
