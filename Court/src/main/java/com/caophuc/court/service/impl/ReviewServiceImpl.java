package com.caophuc.court.service.impl;


import com.caophuc.court.dto.CreateReviewRequest;
import com.caophuc.court.dto.ReviewResponse;
import com.caophuc.court.exception.ResourceNotFoundException;
import com.caophuc.court.model.Court;
import com.caophuc.court.model.Review;
import com.caophuc.court.repository.CourtRepository;
import com.caophuc.court.repository.ReviewRepository;
import com.caophuc.court.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final CourtRepository courtRepository; // Đảm bảo đã inject

    @Override
    @Transactional
    public ReviewResponse addReview(Integer courtId, CreateReviewRequest request, Integer playerId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Sân không tồn tại"));

        Review review = new Review();
        review.setCourt(court);
        review.setPlayerId(playerId);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review savedReview = reviewRepository.save(review);
        updateCourtAverageRating(courtId);

        return mapToReviewResponse(savedReview);
    }

    @Override
    public Page<ReviewResponse> getReviews(Integer courtId, Pageable pageable) {
        if (!courtRepository.existsById(courtId)) {
            throw new ResourceNotFoundException("Sân không tồn tại");
        }
        Page<Review> reviews = reviewRepository.findByCourtId(courtId, pageable);
        return reviews.map(this::mapToReviewResponse);
    }

    @Transactional
    public void updateCourtAverageRating(Integer courtId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Sân không tồn tại"));
        Double avgRating = reviewRepository.calculateAverageRating(courtId); // Gọi từ reviewRepository

        if (avgRating == null) {
            court.setAverageRating(0.0);
        } else {
            court.setAverageRating(Math.round(avgRating * 10.0) / 10.0);
        }
        courtRepository.save(court);
    }

    private ReviewResponse mapToReviewResponse(Review r) {
        ReviewResponse res = ReviewResponse.builder()
                .id(r.getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
        res.setPlayerId(r.getPlayerId());
        return res;
    }
}
