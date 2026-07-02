package ru.adiaphora.platform.application.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ApplicationStatusHistoryJpaRepository extends JpaRepository<ApplicationStatusHistoryEntity, UUID> {

    List<ApplicationStatusHistoryEntity> findByApplicationIdOrderByChangedAtAsc(UUID applicationId);
}
