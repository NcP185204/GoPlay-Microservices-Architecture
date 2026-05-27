package com.caophuc.booking.service.impl;

import com.caophuc.booking.client.CourtClient;
import com.caophuc.booking.client.CourtDto;
import com.caophuc.booking.client.UserClient;
import com.caophuc.booking.client.UserDto;
import com.caophuc.booking.dto.BookingRequest;
import com.caophuc.booking.dto.BookingResponse;
import com.caophuc.booking.dto.NotificationEventDto;
import com.caophuc.booking.exception.ResourceNotFoundException;
import com.caophuc.booking.model.Booking;
import com.caophuc.booking.model.TimeSlot;
import com.caophuc.booking.repository.BookingRepository;
import com.caophuc.booking.repository.TimeSlotRepository;
import com.caophuc.booking.service.BookingService;
import com.caophuc.booking.service.kafka.BookingProducerService;
import com.caophuc.booking.util.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final CourtClient courtClient; // Công cụ gọi sang Court Service
    private final UserClient userClient;   // Công cụ gọi sang Auth/User Service
    
    private final BookingProducerService bookingProducerService; // Kafka Producer

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request, Integer userId) {

        if (request.getTimeSlotIds() == null || request.getTimeSlotIds().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một khung giờ để đặt sân.");
        }

        List<TimeSlot> slotsToBook = new ArrayList<>();
        double totalPrice = 0.0;
        Integer firstCourtId = null;

        for (Integer slotId : request.getTimeSlotIds()) {
            TimeSlot slot = timeSlotRepository.findById(slotId)
                    .orElseThrow(() -> new ResourceNotFoundException("Khung giờ không tồn tại: " + slotId));

            if (firstCourtId == null) {
                firstCourtId = slot.getCourtId(); // Đã sửa thành getCourtId()
            } else if (!Objects.equals(firstCourtId, slot.getCourtId())) {
                throw new IllegalArgumentException("Không thể đặt các khung giờ thuộc về nhiều sân khác nhau trong cùng một đơn hàng.");
            }

            if (!slot.isAvailable()) {
                throw new IllegalStateException("Khung giờ " + slot.getStartTime() + " đã bị người khác đặt!");
            }

            // Đánh dấu là đã được đặt
            slot.setAvailable(false);

            Double price = slot.getPrice() != null ? slot.getPrice() : 0.0;
            totalPrice += price;

            slotsToBook.add(slot);
        }

        // Tạo Booking
        Booking booking = Booking.builder()
                .userId(userId) // Đã sửa thành userId
                .timeSlots(slotsToBook)
                .totalPrice(totalPrice)
                .status(BookingStatus.PENDING) // Luôn luôn là PENDING ở bước này
                .createdAt(LocalDateTime.now())
                .note(request.getNote())
                .build();

        // LƯU Ý: Việc tạo Payment sẽ được thực hiện bởi Payment Service (hoặc một luồng khác) sau khi có Booking ID.
        Booking savedBooking = bookingRepository.save(booking);

        // --- MỚI: PHÁT SỰ KIỆN KAFKA CHO NOTIFICATION SERVICE ---
        try {
            // Gọi Feign Client sang Auth Service để lấy thông tin thực tế của User
            String userEmail = "";
            String fcmToken = "";
            try {
                UserDto userInfo = userClient.getUserById(userId);
                if (userInfo != null) {
                    userEmail = userInfo.getEmail();
                    fcmToken = userInfo.getFcmToken();
                }
            } catch (Exception ex) {
                log.error("Không thể lấy thông tin User từ Auth Service cho userId: {}", userId, ex);
            }

            NotificationEventDto notificationEvent = NotificationEventDto.builder()
                    .userId(userId)
                    .userEmail(userEmail)
                    .fcmToken(fcmToken)
                    .title("Đặt sân thành công!")
                    .content("Bạn đã đặt sân thành công với mã đơn: " + savedBooking.getId() + ". Vui lòng thanh toán trong 15 phút tới.")
                    .type("BOOKING_CREATED")
                    .build();

            // Gửi event sang Kafka (Notification Service sẽ hứng)
            bookingProducerService.sendNotification(notificationEvent);
            
        } catch (Exception e) {
            log.error("Không thể gửi thông báo Kafka khi đặt sân: {}", e.getMessage());
            // Việc lỗi thông báo không nên làm ảnh hưởng đến luồng đặt sân (không throw exception ra ngoài)
        }

        return mapToResponse(savedBooking);
    }

    @Override
    public List<BookingResponse> getUserBookings(Integer userId) {
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return bookings.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public Optional<BookingResponse> getBookingById(Integer bookingId) {
        return bookingRepository.findById(bookingId).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void cancelBooking(Integer bookingId, Integer userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân với ID: " + bookingId));

        if (!Objects.equals(booking.getUserId(), userId)) {
            throw new IllegalArgumentException("Bạn không có quyền hủy đơn đặt sân này.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Đơn này đã được hủy trước đó.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Nhả lại các khung giờ
        for (TimeSlot slot : booking.getTimeSlots()) {
            slot.setAvailable(true);
            timeSlotRepository.save(slot);
        }

        // --- MỚI: PHÁT SỰ KIỆN KAFKA THÔNG BÁO HỦY ---
        try {
            // Lấy email thật
            String userEmail = "";
            try {
                UserDto userInfo = userClient.getUserById(userId);
                if (userInfo != null) {
                    userEmail = userInfo.getEmail();
                }
            } catch (Exception ex) {
                log.error("Không thể lấy thông tin User", ex);
            }
            
            NotificationEventDto notificationEvent = NotificationEventDto.builder()
                    .userId(userId)
                    .userEmail(userEmail)
                    .title("Hủy đặt sân")
                    .content("Đơn đặt sân " + bookingId + " của bạn đã được hủy thành công.")
                    .type("BOOKING_CANCELLED")
                    .build();

            bookingProducerService.sendNotification(notificationEvent);
        } catch (Exception e) {
            log.error("Lỗi gửi Kafka Notification khi hủy đặt sân", e);
        }
    }

    @Override
    public Optional<BookingResponse> getUpcomingBooking(Integer userId) {
        LocalDateTime currentTime = LocalDateTime.now();
        Optional<Booking> upcomingBooking = bookingRepository.findUpcomingBooking(
                userId,
                BookingStatus.CONFIRMED,
                currentTime
        );
        return upcomingBooking.map(this::mapToResponse);
    }

    // Hàm chuyển đổi từ Entity sang DTO
    private BookingResponse mapToResponse(Booking booking) {
        String courtName = "N/A";
        String courtAddress = "N/A";

        if (!booking.getTimeSlots().isEmpty()) {
            Integer courtId = booking.getTimeSlots().get(0).getCourtId();

            // GỌI ĐIỆN SANG COURT SERVICE LẤY THÔNG TIN SÂN
            try {
                CourtDto courtInfo = courtClient.getCourtById(courtId);
                if (courtInfo != null) {
                    courtName = courtInfo.getName();
                    courtAddress = courtInfo.getAddress();
                }
            } catch (Exception e) {
                log.error("Không thể lấy thông tin sân từ Court Service cho courtId: {}", courtId, e);
                courtName = "Sân (Lỗi kết nối)";
            }
        }

        List<String> slotDetails = booking.getTimeSlots().stream()
                .map(slot -> slot.getStartTime().toLocalTime() + " - " + slot.getEndTime().toLocalTime())
                .collect(Collectors.toList());

        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .courtName(courtName) // Tên lấy được từ Court Service
                .courtAddress(courtAddress) // Địa chỉ lấy được từ Court Service
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .timeSlotDetails(slotDetails)
                .build();
    }
}