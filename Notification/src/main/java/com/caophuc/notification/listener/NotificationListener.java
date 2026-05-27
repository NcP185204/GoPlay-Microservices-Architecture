package com.caophuc.notification.listener;

import com.caophuc.notification.dto.NotificationEventDto;
import com.caophuc.notification.model.Notification;
import com.caophuc.notification.repository.NotificationRepository;
import com.caophuc.notification.service.EmailService;
import com.caophuc.notification.service.FirebasePushNotificationService;
import com.caophuc.notification.util.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final EmailService emailService;
    private final FirebasePushNotificationService pushNotificationService;
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "notification-topic", groupId = "notification-group")
    public void consumeNotificationEvent(NotificationEventDto event) {
        log.info(" Nhận được message từ Kafka (Topic: notification-topic): {}", event);

        try {
            // 1. Lưu thông báo vào Database (để App có thể xem lại trong List Notification)
            saveNotificationToDatabase(event);

            // 2. Gửi Email nếu có
            if (event.getUserEmail() != null && !event.getUserEmail().trim().isEmpty()) {
                log.info("Bắt đầu xử lý gửi Email cho user: {}", event.getUserEmail());
                emailService.sendEmail(
                        event.getUserEmail(),
                        event.getTitle(),
                        event.getContent()
                );
            }

            // 3. Gửi Push Notification (Firebase) nếu có FCM Token
            if (event.getFcmToken() != null && !event.getFcmToken().trim().isEmpty()) {
                log.info("Bắt đầu xử lý gửi Push Notification (FCM) cho token: {}", event.getFcmToken());
                pushNotificationService.sendPushNotification(
                        event.getFcmToken(),
                        event.getTitle(),
                        event.getContent()
                );
            }

            log.info(" Xử lý thành công message cho userId: {}", event.getUserId());
        } catch (Exception e) {
            log.error(" Lỗi trong quá trình xử lý Notification Message (UserId: {}). Lỗi: {}", event.getUserId(), e.getMessage(), e);
            // Ghi chú: Nếu hệ thống cần tính tin cậy cao, ở đây có thể xem xét gửi message này 
            // sang một Dead Letter Queue (DLQ) Kafka topic để xử lý lại sau.
        }
    }

    private void saveNotificationToDatabase(NotificationEventDto event) {
        try {
            Notification notification = Notification.builder()
                    .user(event.getUserId())
                    .title(event.getTitle())
                    .content(event.getContent())
                    // Chuyển string type từ Kafka về Enum (nếu không map được sẽ báo lỗi, tùy logic hệ thống)
                    .type(mapType(event.getType()))
                    .isRead(false)
                    // Nếu sau này DTO có thêm field relatedId thì đưa vào đây
                    // .relatedId(event.getRelatedId()) 
                    .build();

            notificationRepository.save(notification);
            log.info("Đã lưu Notification vào Database cho userId: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Lỗi khi lưu Notification vào Database. Lỗi: {}", e.getMessage());
            throw e; // Ném ra lỗi để khối try-catch ngoài cùng (Kafka listener) in ra lỗi tổng
        }
    }

    private NotificationType mapType(String typeStr) {
        try {
            if (typeStr == null || typeStr.isEmpty()) return NotificationType.SYSTEM; // Default
            return NotificationType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Không tìm thấy NotificationType phù hợp với chuỗi: '{}'. Chuyển về mặc định SYSTEM.", typeStr);
            return NotificationType.SYSTEM;
        }
    }
}
