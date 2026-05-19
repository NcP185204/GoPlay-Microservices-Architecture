package com.caophuc.notification.repository;

import com.caophuc.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    
    // Sửa tên hàm vì trong entity Notification, thuộc tính tên là "user" chứ không phải "userId"
    // Spring Data JPA sẽ tự động build query WHERE n.user = ?
    Page<Notification> findByUserOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    // Đếm số thông báo chưa đọc
    long countByUserAndIsReadFalse(Integer userId);
}
