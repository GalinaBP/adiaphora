package ru.adiaphora.platform.review.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.adiaphora.platform.review.domain.Review;
import ru.adiaphora.platform.review.domain.ReviewRepository;
import ru.adiaphora.platform.review.domain.ReviewStatus;
import ru.adiaphora.platform.review.domain.RouteOverride;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Adapts Spring Data JPA to the {@link ReviewRepository} port, appending drained route overrides. */
@Component
class ReviewRepositoryAdapter implements ReviewRepository {

    private static final Set<ReviewStatus> ACTIVE = Set.of(ReviewStatus.OPEN, ReviewStatus.ASSIGNED,
            ReviewStatus.WAITING_FOR_INFORMATION, ReviewStatus.IN_PROGRESS);

    private final ReviewJpaRepository reviews;
    private final ReviewRouteOverrideJpaRepository overrides;

    ReviewRepositoryAdapter(ReviewJpaRepository reviews, ReviewRouteOverrideJpaRepository overrides) {
        this.reviews = reviews;
        this.overrides = overrides;
    }

    @Override
    public Review save(Review review) {
        ReviewEntity entity = reviews.findById(review.id())
                .map(existing -> {
                    existing.applyFrom(review);
                    return existing;
                })
                .orElseGet(() -> ReviewEntity.fromDomain(review));
        reviews.save(entity);

        List<RouteOverride> newOverrides = review.drainNewOverrides();
        if (!newOverrides.isEmpty()) {
            overrides.saveAll(newOverrides.stream()
                    .map(override -> ReviewRouteOverrideEntity.of(review.id(), override))
                    .toList());
        }
        return review;
    }

    @Override
    public Optional<Review> findById(UUID reviewId) {
        return reviews.findById(reviewId).map(ReviewEntity::toDomain);
    }

    @Override
    public Page<Review> findAll(Pageable pageable) {
        return reviews.findAll(pageable).map(ReviewEntity::toDomain);
    }

    @Override
    public boolean existsActiveByApplicationId(UUID applicationId) {
        return reviews.existsByApplicationIdAndStatusIn(applicationId, ACTIVE);
    }

    @Override
    public List<RouteOverride> findOverrides(UUID reviewId) {
        return overrides.findByReviewIdOrderByReviewedAtAsc(reviewId).stream()
                .map(ReviewRouteOverrideEntity::toDomain)
                .toList();
    }
}
