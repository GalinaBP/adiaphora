package ru.adiaphora.platform.questionnaire.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.common.persistence.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "question_answers")
class QuestionAnswerEntity extends BaseEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "response_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID responseId;

    @Column(name = "question_code", nullable = false, length = 64, updatable = false)
    private String questionCode;

    @Column(name = "value", length = 4000)
    private String value;

    protected QuestionAnswerEntity() {
    }

    QuestionAnswerEntity(UUID id, UUID responseId, String questionCode, String value) {
        this.id = id;
        this.responseId = responseId;
        this.questionCode = questionCode;
        this.value = value;
    }

    String getQuestionCode() {
        return questionCode;
    }

    String getValue() {
        return value;
    }

    void setValue(String value) {
        this.value = value;
    }
}
