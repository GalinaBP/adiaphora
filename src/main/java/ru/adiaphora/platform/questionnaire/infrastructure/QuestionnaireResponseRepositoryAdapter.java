package ru.adiaphora.platform.questionnaire.infrastructure;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireResponse;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireResponseRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Adapts Spring Data JPA to the {@link QuestionnaireResponseRepository} port, upserting answers. */
@Component
class QuestionnaireResponseRepositoryAdapter implements QuestionnaireResponseRepository {

    private final QuestionnaireResponseJpaRepository responses;
    private final QuestionAnswerJpaRepository answers;

    QuestionnaireResponseRepositoryAdapter(QuestionnaireResponseJpaRepository responses,
                                           QuestionAnswerJpaRepository answers) {
        this.responses = responses;
        this.answers = answers;
    }

    @Override
    public Optional<QuestionnaireResponse> findByApplicationId(UUID applicationId) {
        return responses.findByApplicationId(applicationId).map(this::toDomain);
    }

    @Override
    public QuestionnaireResponse save(QuestionnaireResponse response) {
        QuestionnaireResponseEntity entity = responses.findById(response.id())
                .orElseGet(() -> new QuestionnaireResponseEntity(response.id(), response.applicationId(),
                        response.versionId(), response.versionCode()));
        responses.save(entity);

        Map<String, QuestionAnswerEntity> existing = answers.findByResponseId(entity.getId()).stream()
                .collect(Collectors.toMap(QuestionAnswerEntity::getQuestionCode, Function.identity()));

        response.answers().forEach((code, value) -> {
            QuestionAnswerEntity answer = existing.get(code);
            if (answer != null) {
                answer.setValue(value);
                answers.save(answer);
            } else {
                answers.save(new QuestionAnswerEntity(UUID.randomUUID(), entity.getId(), code, value));
            }
        });

        return response;
    }

    private QuestionnaireResponse toDomain(QuestionnaireResponseEntity entity) {
        Map<String, String> answerMap = new HashMap<>();
        answers.findByResponseId(entity.getId())
                .forEach(answer -> answerMap.put(answer.getQuestionCode(), answer.getValue()));
        return QuestionnaireResponse.rehydrate(entity.getId(), entity.getApplicationId(),
                entity.getVersionId(), entity.getVersionCode(), answerMap);
    }
}
