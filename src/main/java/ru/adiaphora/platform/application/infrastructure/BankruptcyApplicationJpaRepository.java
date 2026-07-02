package ru.adiaphora.platform.application.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface BankruptcyApplicationJpaRepository extends JpaRepository<BankruptcyApplicationEntity, UUID> {

    Page<BankruptcyApplicationEntity> findByOwnerId(UUID ownerId, Pageable pageable);
}
