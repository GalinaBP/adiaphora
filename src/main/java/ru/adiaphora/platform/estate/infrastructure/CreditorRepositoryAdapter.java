package ru.adiaphora.platform.estate.infrastructure;

import org.springframework.stereotype.Component;
import ru.adiaphora.platform.estate.domain.Creditor;
import ru.adiaphora.platform.estate.domain.CreditorRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapts Spring Data JPA to the domain {@link CreditorRepository}. Upserts on save. */
@Component
class CreditorRepositoryAdapter implements CreditorRepository {

    private final CreditorJpaRepository jpa;

    CreditorRepositoryAdapter(CreditorJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Creditor save(Creditor creditor) {
        CreditorEntity entity = jpa.findById(creditor.id())
                .map(existing -> {
                    existing.applyFrom(creditor);
                    return existing;
                })
                .orElseGet(() -> CreditorEntity.fromDomain(creditor));
        return jpa.save(entity).toDomain();
    }

    @Override
    public Optional<Creditor> findById(UUID id) {
        return jpa.findById(id).map(CreditorEntity::toDomain);
    }

    @Override
    public List<Creditor> findByApplicationId(UUID applicationId) {
        return jpa.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream()
                .map(CreditorEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}
