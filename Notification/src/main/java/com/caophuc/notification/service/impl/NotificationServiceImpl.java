package com.caophuc.notification.service.impl;

import com.caophuc.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    // Hiện tại NotificationServiceImpl không cần thiết làm gì nữa
    // vì NotificationListener đã lo toàn bộ quy trình:
    // 1. Nhận Kafka Message
    // 2. Lưu DB
    // 3. Gọi EmailService
    // 4. Gọi FirebasePushNotificationService

    // Bạn có thể giữ lại class này để viết các hàm liên quan đến business logic khác
    // ví dụ: dọn dẹp (xóa) thông báo cũ, gửi thông báo nhắc lịch hàng loạt bằng cronjob...
}
