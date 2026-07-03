package ru.adiaphora.platform.review.application;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.adiaphora.platform.common.error.ResourceNotFoundException;
import ru.adiaphora.platform.common.web.PageResponse;
import ru.adiaphora.platform.review.domain.ReviewRepository;

import java.util.UUID;

/** Read access to reviews for staff roles (URL-secured). */
@Service
public class ReviewQueries {

    private final ReviewRepository reviews;

    public ReviewQueries(ReviewRepository reviews) {
        this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewView> list(Pageable pageable) {
        return PageResponse.from(reviews.findAll(pageable).map(ReviewView::from));
    }

    @Transactional(readOnly = true)
    public ReviewView get(UUID reviewId) {
        return reviews.findById(reviewId)
                .map(ReviewView::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Review", reviewId));
    }
}
