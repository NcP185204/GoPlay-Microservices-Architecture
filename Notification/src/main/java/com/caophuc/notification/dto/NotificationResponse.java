package com.caophuc.notification.dto;
import com.caophuc.notification.util.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private Integer id;
    private String title;
    private String content;
    private NotificationType type;
    private Integer relatedId;
    private boolean isRead;
    private LocalDateTime createdAt;
}
