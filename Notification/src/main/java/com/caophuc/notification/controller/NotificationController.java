package com.caophuc.notification.controller;

import com.caophuc.notification.dto.NotificationResponse;
import com.caophuc.notification.model.Notification;
import com.caophuc.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    // 1. Lấy danh sách thông báo của tôi (Frontend dùng để vẽ List)
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @RequestHeader("X-User-Id") Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        // Lấy Page<Notification> từ DB
        Page<Notification> notificationPage = notificationRepository.findByUserOrderByCreatedAtDesc(userId, pageRequest);

        // Dùng hàm map() của Page để chuyển đổi từng Notification sang NotificationResponse
        Page<NotificationResponse> responsePage = notificationPage.map(this::mapToResponse);

        return ResponseEntity.ok(responsePage);
    }

    // 2. Đếm số thông báo chưa đọc (Frontend dùng để vẽ cái chấm đỏ)
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@RequestHeader("X-User-Id") Integer userId) {
        return ResponseEntity.ok(notificationRepository.countByUserAndIsReadFalse(userId));
    }

    // 3. Đánh dấu đã đọc 1 thông báo
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer id, @RequestHeader("X-User-Id") Integer userId) {
        notificationRepository.findById(id).ifPresent(notification -> {
            // Kiểm tra bảo mật: Chỉ cho phép đọc thông báo của chính mình
            if (notification.getUser().equals(userId)) {
                notification.setRead(true);
                notificationRepository.save(notification);
            }
        });
        return ResponseEntity.ok().build();
    }

    // 4. Đánh dấu đã đọc TẤT CẢ thông báo
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestHeader("X-User-Id") Integer userId) {
        // Lấy tất cả thông báo chưa đọc của user này
        List<Notification> unreadNotifications = notificationRepository.findByUserOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .getContent()
                .stream()
                .filter(n -> !n.isRead())
                .toList();
                
        // Đổi trạng thái thành true
        unreadNotifications.forEach(n -> n.setRead(true));
        
        // Lưu hàng loạt
        notificationRepository.saveAll(unreadNotifications);
        
        return ResponseEntity.ok().build();
    }

    // Hàm helper để chuyển đổi từ Entity sang DTO
    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .relatedId(notification.getRelatedId())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
