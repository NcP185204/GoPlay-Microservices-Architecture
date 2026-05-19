package com.caophuc.booking.service.kafka;

import com.caophuc.booking.dto.NotificationEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Gửi yêu cầu thông báo tới Notification Service thông qua Kafka.
     *
     * @param event Thông tin về sự kiện cần thông báo.
     */
    public void sendNotification(NotificationEventDto event) {
        log.info("Đang gửi Kafka Event tới topic 'notification-topic' cho User ID: {}", event.getUserId());
        try {
            kafkaTemplate.send("notification-topic", event);
            log.info("Gửi sự kiện thông báo thành công!");
        } catch (Exception e) {
            log.error("Lỗi khi gửi sự kiện thông báo qua Kafka: {}", e.getMessage());
        }
    }
}