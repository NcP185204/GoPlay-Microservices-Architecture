package com.caophuc.court.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {

    private Integer id;

    // ID của người đánh giá
    private Integer playerId;

    // Nội dung đánh giá
    private int rating;
    private String comment;

    // Thời điểm đánh giá
    private LocalDateTime createdAt;
}
