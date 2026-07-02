package ru.adiaphora.platform.questionnaire.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.common.persistence.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "questionnaire_versions")
class QuestionnaireVersionEntity extends BaseEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private VersionStatus status;

    protected QuestionnaireVersionEntity() {
    }

    QuestionnaireVersionEntity(UUID id, String code, String label, VersionStatus status) {
        this.id = id;
        this.code = code;
        this.label = label;
        this.status = status;
    }

    UUID getId() {
        return id;
    }

    String getCode() {
        return code;
    }

    String getLabel() {
        return label;
    }
}
