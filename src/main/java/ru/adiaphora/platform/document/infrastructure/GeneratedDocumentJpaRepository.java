package ru.adiaphora.platform.document.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface GeneratedDocumentJpaRepository extends JpaRepository<GeneratedDocumentEntity, UUID> {

    List<GeneratedDocumentEntity> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}
