package com.caophuc.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationEventDto {
    private Integer userId;
    private String userEmail;
    private String fcmToken;
    private String title;
    private String content;
    private String type; // Thường dùng String để map dễ dàng giữa các service thay vì dùng Enum cứng
}
