package ru.adiaphora.platform.review.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ReviewRouteOverrideJpaRepository extends JpaRepository<ReviewRouteOverrideEntity, UUID> {

    List<ReviewRouteOverrideEntity> findByReviewIdOrderByReviewedAtAsc(UUID reviewId);
}
