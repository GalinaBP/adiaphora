package ru.adiaphora.platform.estate.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.common.persistence.BaseEntity;
import ru.adiaphora.platform.estate.domain.Creditor;
import ru.adiaphora.platform.estate.domain.CreditorDetails;
import ru.adiaphora.platform.estate.domain.CreditorType;

import java.math.BigDecimal;
import java.util.UUID;

/** JPA persistence view of the {@link Creditor} aggregate ({@code creditors} table, V010). */
@Entity
@Table(name = "creditors")
class CreditorEntity extends BaseEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "application_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID applicationId;

    @Column(name = "name", nullable = false, length = 300)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "creditor_type", nullable = false, length = 48)
    private CreditorType creditorType;

    @Column(name = "inn", length = 12)
    private String inn;

    @Column(name = "claim_basis", length = 500)
    private String claimBasis;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "principal_amount", precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_amount", precision = 19, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "penalty_amount", precision = 19, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "overdue", nullable = false)
    private boolean overdue;

    @Column(name = "secured", nullable = false)
    private boolean secured;

    protected CreditorEntity() {
        // for JPA
    }

    static CreditorEntity fromDomain(Creditor creditor) {
        CreditorEntity entity = new CreditorEntity();
        entity.id = creditor.id();
        entity.applicationId = creditor.applicationId();
        entity.applyFrom(creditor);
        return entity;
    }

    void applyFrom(Creditor creditor) {
        CreditorDetails d = creditor.details();
        this.name = d.name();
        this.creditorType = d.type();
        this.inn = d.inn();
        this.claimBasis = d.claimBasis();
        this.currency = d.currency();
        this.totalAmount = d.totalAmount();
        this.principalAmount = d.principalAmount();
        this.interestAmount = d.interestAmount();
        this.penaltyAmount = d.penaltyAmount();
        this.overdue = d.overdue();
        this.secured = d.secured();
    }

    Creditor toDomain() {
        return Creditor.rehydrate(id, applicationId, new CreditorDetails(
                name, creditorType, inn, claimBasis, currency,
                totalAmount, principalAmount, interestAmount, penaltyAmount, overdue, secured));
    }
}
