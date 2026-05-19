package com.caophuc.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventDto {
    private Integer userId;        // ID của người nhận
    private String userEmail;      // Email (dành cho gửi Email)
    private String fcmToken;       // Token thiết bị (dành cho Push Notification Firebase, nếu có)
    private String title;          // Tiêu đề thông báo
    private String content;        // Nội dung thông báo
    private String type;           // Loại thông báo (VD: "BOOKING_CREATED", "PAYMENT_SUCCESS")
}