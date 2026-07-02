package ru.adiaphora.platform.questionnaire.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.questionnaire.domain.QuestionOption;

import java.util.UUID;

@Entity
@Table(name = "question_options")
class QuestionOptionEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "question_definition_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID questionDefinitionId;

    @Column(name = "value", nullable = false, length = 128)
    private String value;

    @Column(name = "label", nullable = false, length = 300)
    private String label;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected QuestionOptionEntity() {
    }

    QuestionOptionEntity(UUID id, UUID questionDefinitionId, String value, String label, int displayOrder) {
        this.id = id;
        this.questionDefinitionId = questionDefinitionId;
        this.value = value;
        this.label = label;
        this.displayOrder = displayOrder;
    }

    UUID getQuestionDefinitionId() {
        return questionDefinitionId;
    }

    QuestionOption toDomain() {
        return new QuestionOption(value, label, displayOrder);
    }
}
