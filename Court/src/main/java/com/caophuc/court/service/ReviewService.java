package com.caophuc.court.service;


import com.caophuc.court.dto.CreateReviewRequest;
import com.caophuc.court.dto.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    /**
     * Thêm một đánh giá mới cho một sân.
     * @param courtId ID của sân được đánh giá.
     * @param request DTO chứa thông tin đánh giá.
     * @param playerId ID Người dùng đang thực hiện đánh giá.
     * @return DTO của đánh giá vừa được tạo.
     */
    ReviewResponse addReview(Integer courtId, CreateReviewRequest request, Integer playerId);

    /**
     * Lấy danh sách các đánh giá của một sân (có phân trang).
     * @param courtId ID của sân.
     * @param pageable Thông tin phân trang.
     * @return Một trang các đánh giá.
     */
    Page<ReviewResponse> getReviews(Integer courtId, Pageable pageable);
}
