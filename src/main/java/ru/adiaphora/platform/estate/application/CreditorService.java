package ru.adiaphora.platform.estate.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.estate.domain.Creditor;
import ru.adiaphora.platform.estate.domain.CreditorDetails;
import ru.adiaphora.platform.estate.domain.CreditorRepository;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for creditors, each scoped to an application the caller is allowed to access. Create/update
 * flag likely duplicates (same name + INN in the same case) without blocking them.
 */
@Service
public class CreditorService {

    private final CreditorRepository creditors;
    private final EstateAccess access;

    public CreditorService(CreditorRepository creditors, EstateAccess access) {
        this.creditors = creditors;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<Creditor> list(UUID applicationId) {
        access.requireAccess(applicationId);
        return creditors.findByApplicationId(applicationId);
    }

    @Transactional(readOnly = true)
    public Creditor get(UUID applicationId, UUID creditorId) {
        access.requireAccess(applicationId);
        return loadInApplication(applicationId, creditorId);
    }

    @Transactional
    public Saved<Creditor> create(UUID applicationId, CreditorDetails details) {
        access.requireAccess(applicationId);
        boolean duplicate = isDuplicate(applicationId, null, details);
        Creditor creditor = creditors.save(Creditor.create(UUID.randomUUID(), applicationId, details));
        return new Saved<>(creditor, duplicate);
    }

    @Transactional
    public Saved<Creditor> update(UUID applicationId, UUID creditorId, CreditorDetails details) {
        access.requireAccess(applicationId);
        Creditor creditor = loadInApplication(applicationId, creditorId);
        boolean duplicate = isDuplicate(applicationId, creditorId, details);
        creditor.update(details);
        return new Saved<>(creditors.save(creditor), duplicate);
    }

    @Transactional
    public void delete(UUID applicationId, UUID creditorId) {
        access.requireAccess(applicationId);
        Creditor creditor = loadInApplication(applicationId, creditorId);
        creditors.deleteById(creditor.id());
    }

    private Creditor loadInApplication(UUID applicationId, UUID creditorId) {
        Creditor creditor = creditors.findById(creditorId)
                .filter(c -> c.belongsTo(applicationId))
                .orElseThrow(() -> ResourceNotFoundException.of("Creditor", creditorId));
        return creditor;
    }

    /** A duplicate is another creditor (id != excludeId) in the same case with the same key. */
    private boolean isDuplicate(UUID applicationId, UUID excludeId, CreditorDetails details) {
        String key = details.duplicateKey();
        return creditors.findByApplicationId(applicationId).stream()
                .filter(c -> !c.id().equals(excludeId))
                .anyMatch(c -> c.details().duplicateKey().equals(key));
    }
}
