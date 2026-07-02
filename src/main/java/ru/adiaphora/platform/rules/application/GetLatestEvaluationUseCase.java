package ru.adiaphora.platform.rules.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.rules.api.RulesEvaluationResult;
import ru.adiaphora.platform.rules.domain.RuleEvaluationRepository;

import java.util.UUID;

/** Returns the most recent evaluation for an application (after an ownership check). */
@Service
public class GetLatestEvaluationUseCase {

    private final RulesAccess access;
    private final RuleEvaluationRepository repository;

    public GetLatestEvaluationUseCase(RulesAccess access, RuleEvaluationRepository repository) {
        this.access = access;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public RulesEvaluationResult latest(UUID applicationId) {
        access.requireAccess(applicationId);
        return repository.findLatestByApplicationId(applicationId)
                .map(RulesResultMapper::toResult)
                .orElseThrow(() -> ResourceNotFoundException.of("Evaluation for application", applicationId));
    }
}
