package ru.adiaphora.platform.questionnaire.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.common.persistence.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "questionnaire_responses")
class QuestionnaireResponseEntity extends BaseEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "application_id", columnDefinition = "BINARY(16)", nullable = false, unique = true,
            updatable = false)
    private UUID applicationId;

    @Column(name = "version_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID versionId;

    @Column(name = "version_code", nullable = false, length = 64)
    private String versionCode;

    protected QuestionnaireResponseEntity() {
    }

    QuestionnaireResponseEntity(UUID id, UUID applicationId, UUID versionId, String versionCode) {
        this.id = id;
        this.applicationId = applicationId;
        this.versionId = versionId;
        this.versionCode = versionCode;
    }

    UUID getId() {
        return id;
    }

    UUID getApplicationId() {
        return applicationId;
    }

    UUID getVersionId() {
        return versionId;
    }

    String getVersionCode() {
        return versionCode;
    }
}
