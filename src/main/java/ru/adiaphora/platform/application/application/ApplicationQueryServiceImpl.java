package ru.adiaphora.platform.application.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.application.api.ApplicationQueryService;
import ru.adiaphora.platform.application.api.ApplicationView;
import ru.adiaphora.platform.application.domain.BankruptcyApplicationRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Module-facing implementation of {@link ApplicationQueryService}. Intended for other modules (rules,
 * review, document) that already operate under their own authorization; it performs no owner check.
 */
@Service
class ApplicationQueryServiceImpl implements ApplicationQueryService {

    private final BankruptcyApplicationRepository repository;

    ApplicationQueryServiceImpl(BankruptcyApplicationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApplicationView> findById(UUID applicationId) {
        return repository.findById(applicationId).map(ApplicationMapper::toView);
    }
}
