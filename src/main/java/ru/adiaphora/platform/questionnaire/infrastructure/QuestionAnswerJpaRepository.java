package ru.adiaphora.platform.questionnaire.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface QuestionAnswerJpaRepository extends JpaRepository<QuestionAnswerEntity, UUID> {

    List<QuestionAnswerEntity> findByResponseId(UUID responseId);
}
