package ru.adiaphora.platform.estate.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface CreditorJpaRepository extends JpaRepository<CreditorEntity, UUID> {

    List<CreditorEntity> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}
