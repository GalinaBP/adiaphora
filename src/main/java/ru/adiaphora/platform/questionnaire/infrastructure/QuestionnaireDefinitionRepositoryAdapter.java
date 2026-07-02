package ru.adiaphora.platform.questionnaire.infrastructure;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.questionnaire.domain.QuestionDefinition;
import ru.adiaphora.platform.questionnaire.domain.QuestionOption;
import ru.adiaphora.platform.questionnaire.domain.QuestionSection;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireDefinition;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireDefinitionRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Assembles a {@link QuestionnaireDefinition} from its version, sections, questions, and options. */
@Component
class QuestionnaireDefinitionRepositoryAdapter implements QuestionnaireDefinitionRepository {

    private final QuestionnaireVersionJpaRepository versions;
    private final QuestionSectionJpaRepository sections;
    private final QuestionDefinitionJpaRepository questions;
    private final QuestionOptionJpaRepository options;

    QuestionnaireDefinitionRepositoryAdapter(QuestionnaireVersionJpaRepository versions,
                                             QuestionSectionJpaRepository sections,
                                             QuestionDefinitionJpaRepository questions,
                                             QuestionOptionJpaRepository options) {
        this.versions = versions;
        this.sections = sections;
        this.questions = questions;
        this.options = options;
    }

    @Override
    public Optional<QuestionnaireDefinition> findActive() {
        return versions.findFirstByStatus(VersionStatus.ACTIVE).map(this::assemble);
    }

    @Override
    public Optional<QuestionnaireDefinition> findByVersionId(UUID versionId) {
        return versions.findById(versionId).map(this::assemble);
    }

    private QuestionnaireDefinition assemble(QuestionnaireVersionEntity version) {
        List<QuestionSection> sectionList = sections
                .findByVersionIdOrderByDisplayOrderAsc(version.getId()).stream()
                .map(QuestionSectionEntity::toDomain)
                .toList();

        List<QuestionDefinitionEntity> questionEntities = questions.findByVersionId(version.getId());
        List<UUID> questionIds = questionEntities.stream()
                .map(QuestionDefinitionEntity::getId)
                .toList();

        Map<UUID, List<QuestionOption>> optionsByQuestion = questionIds.isEmpty()
                ? Map.of()
                : options.findByQuestionDefinitionIdInOrderByDisplayOrderAsc(questionIds).stream()
                        .collect(Collectors.groupingBy(
                                QuestionOptionEntity::getQuestionDefinitionId,
                                Collectors.mapping(QuestionOptionEntity::toDomain, Collectors.toList())));

        List<QuestionDefinition> questionList = questionEntities.stream()
                .map(entity -> entity.toDomain(optionsByQuestion.getOrDefault(entity.getId(), List.of())))
                .toList();

        return new QuestionnaireDefinition(version.getId(), version.getCode(), version.getLabel(),
                sectionList, questionList);
    }
}
