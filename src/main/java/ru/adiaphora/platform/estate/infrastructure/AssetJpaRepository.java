package ru.adiaphora.platform.estate.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AssetJpaRepository extends JpaRepository<AssetEntity, UUID> {

    List<AssetEntity> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}
