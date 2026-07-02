package ru.adiaphora.platform.questionnaire.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.questionnaire.domain.QuestionDefinition;
import ru.adiaphora.platform.questionnaire.domain.QuestionOption;
import ru.adiaphora.platform.questionnaire.domain.QuestionType;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "question_definitions")
class QuestionDefinitionEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "version_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID versionId;

    @Column(name = "section_code", nullable = false, length = 64)
    private String sectionCode;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private QuestionType type;

    @Column(name = "label", nullable = false, length = 500)
    private String label;

    @Column(name = "help_text", length = 1000)
    private String helpText;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "validation_configuration", length = 2000)
    private String validationConfiguration;

    protected QuestionDefinitionEntity() {
    }

    QuestionDefinitionEntity(UUID id, UUID versionId, String sectionCode, String code, QuestionType type,
                             String label, String helpText, boolean required, int displayOrder,
                             String validationConfiguration) {
        this.id = id;
        this.versionId = versionId;
        this.sectionCode = sectionCode;
        this.code = code;
        this.type = type;
        this.label = label;
        this.helpText = helpText;
        this.required = required;
        this.displayOrder = displayOrder;
        this.validationConfiguration = validationConfiguration;
    }

    UUID getId() {
        return id;
    }

    QuestionDefinition toDomain(List<QuestionOption> options) {
        return new QuestionDefinition(code, sectionCode, type, label, helpText, required, displayOrder,
                validationConfiguration, options);
    }
}
