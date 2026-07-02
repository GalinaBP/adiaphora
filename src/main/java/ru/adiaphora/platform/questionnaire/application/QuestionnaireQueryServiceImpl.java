package ru.adiaphora.platform.questionnaire.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.questionnaire.api.QuestionnaireQueryService;
import ru.adiaphora.platform.questionnaire.api.QuestionnaireSnapshot;
import ru.adiaphora.platform.questionnaire.domain.QuestionnaireResponseRepository;

import java.util.Optional;
import java.util.UUID;

/** Module-facing implementation producing the answer snapshot consumed by the rules engine. */
@Service
class QuestionnaireQueryServiceImpl implements QuestionnaireQueryService {

    private final QuestionnaireResponseRepository responses;

    QuestionnaireQueryServiceImpl(QuestionnaireResponseRepository responses) {
        this.responses = responses;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuestionnaireSnapshot> snapshot(UUID applicationId) {
        return responses.findByApplicationId(applicationId)
                .map(response -> new QuestionnaireSnapshot(applicationId, response.versionCode(),
                        response.answers()));
    }
}
