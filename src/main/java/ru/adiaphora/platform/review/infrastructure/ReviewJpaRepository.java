package ru.adiaphora.platform.review.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.adiaphora.platform.review.domain.ReviewStatus;

import java.util.Collection;
import java.util.UUID;

interface ReviewJpaRepository extends JpaRepository<ReviewEntity, UUID> {

    boolean existsByApplicationIdAndStatusIn(UUID applicationId, Collection<ReviewStatus> statuses);
}
