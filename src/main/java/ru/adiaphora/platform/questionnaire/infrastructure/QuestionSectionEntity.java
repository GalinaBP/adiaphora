package ru.adiaphora.platform.questionnaire.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.questionnaire.domain.QuestionSection;

import java.util.UUID;

@Entity
@Table(name = "question_sections")
class QuestionSectionEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "version_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID versionId;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected QuestionSectionEntity() {
    }

    QuestionSectionEntity(UUID id, UUID versionId, String code, String title, int displayOrder) {
        this.id = id;
        this.versionId = versionId;
        this.code = code;
        this.title = title;
        this.displayOrder = displayOrder;
    }

    QuestionSection toDomain() {
        return new QuestionSection(code, title, displayOrder);
    }
}
