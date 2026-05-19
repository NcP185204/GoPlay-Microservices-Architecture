package com.caophuc.booking.repository;

import com.caophuc.booking.model.Booking;
import com.caophuc.booking.util.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(Integer userId);

    /**
     * Tìm kiếm trận đấu sắp tới gần nhất của một người dùng.
     * @param userId ID của người dùng
     * @param status Trạng thái đơn hàng (ví dụ: CONFIRMED)
     * @param currentTime Thời gian hiện tại, để lọc các trận trong tương lai
     * @return Optional chứa Booking nếu tìm thấy
     */
    @Query("SELECT b FROM Booking b JOIN b.timeSlots ts " +
           "WHERE b.userId = :userId " +
           "AND b.status = :status " +
           "AND ts.startTime > :currentTime " +
           "ORDER BY ts.startTime ASC " +
           "LIMIT 1")
    Optional<Booking> findUpcomingBooking(
            @Param("userId") Integer userId,
            @Param("status") BookingStatus status,
            @Param("currentTime") LocalDateTime currentTime
    );
}