package com.caophuc.booking.model;

import com.caophuc.booking.util.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;



    @Column(name = "user_id", nullable = false)
    private Integer userId;

    // 2. Đặt những khung giờ nào? (1 Đơn hàng có nhiều Slot) -> Phải là @ManyToMany
    @ManyToMany
    @JoinTable(
        name = "booking_time_slots",
        joinColumns = @JoinColumn(name = "booking_id"),
        inverseJoinColumns = @JoinColumn(name = "time_slot_id")
    )
    private List<TimeSlot> timeSlots;

    // 3. Tổng tiền
    @Column(nullable = false)
    private Double totalPrice;

    // 4. Trạng thái đơn hàng (QUAN TRỌNG)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    // 5. Ngày tạo đơn
    @Column(nullable = false)
    private LocalDateTime createdAt;


    private String note;


}
