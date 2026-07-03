package ru.adiaphora.platform.review.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Domain port for reviews and their route-override history. */
public interface ReviewRepository {

    Review save(Review review);

    Optional<Review> findById(UUID reviewId);

    Page<Review> findAll(Pageable pageable);

    boolean existsActiveByApplicationId(UUID applicationId);

    List<RouteOverride> findOverrides(UUID reviewId);
}
