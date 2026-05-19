package com.caophuc.booking.service.kafka;

import com.caophuc.booking.client.UserClient;
import com.caophuc.booking.client.UserDto;
import com.caophuc.booking.dto.NotificationEventDto;
import com.caophuc.booking.dto.PaymentEventDto;
import com.caophuc.booking.exception.ResourceNotFoundException;
import com.caophuc.booking.model.Booking;
import com.caophuc.booking.repository.BookingRepository;
import com.caophuc.booking.util.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentConsumerService {

    private final BookingRepository bookingRepository;
    private final BookingProducerService bookingProducerService;
    private final UserClient userClient;

    /**
     * Lắng nghe CÁC sự kiện thanh toán từ Payment Service thông qua một topic duy nhất.
     * Sử dụng trường 'status' trong eventDto để phân loại và xử lý.
     */
    @KafkaListener(topics = "payment-events-topic", groupId = "booking-service-group")
    @Transactional
    public void handlePaymentEvents(PaymentEventDto eventDto) {
        log.info("Nhận được sự kiện thanh toán từ Payment Service cho Booking ID: {} với trạng thái: {}", eventDto.getBookingId(), eventDto.getStatus());
        
        try {
            if ("SUCCESS".equalsIgnoreCase(eventDto.getStatus())) {
                processPaymentSuccess(eventDto);
            } else if ("FAILED".equalsIgnoreCase(eventDto.getStatus())) {
                processPaymentFailure(eventDto);
            } else {
                log.warn("Trạng thái thanh toán không hợp lệ: {}", eventDto.getStatus());
            }
        } catch (Exception e) {
            log.error("Lỗi khi phân loại và xử lý sự kiện thanh toán: {}", e.getMessage());
        }
    }

    private void processPaymentSuccess(PaymentEventDto eventDto) {
        Integer bookingId = eventDto.getBookingId();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Booking ID: " + bookingId));

        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            log.info("Đã cập nhật trạng thái đơn đặt sân {} thành CONFIRMED.", bookingId);
            
            // Bắn thông báo Kafka báo thanh toán thành công
            sendNotificationEvent(booking, "Thanh toán thành công!", "Đơn hàng " + bookingId + " đã được thanh toán.", "PAYMENT_SUCCESS");
            
        } else {
            log.warn("Đơn đặt sân {} không ở trạng thái PENDING (Hiện tại: {}).", bookingId, booking.getStatus());
        }
    }

    private void processPaymentFailure(PaymentEventDto eventDto) {
        Integer bookingId = eventDto.getBookingId();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Booking ID: " + bookingId));

        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            
            // Giải phóng lại các khung giờ (TimeSlot) cho người khác đặt
            booking.getTimeSlots().forEach(slot -> slot.setAvailable(true));
            
            log.info("Đã hủy đơn đặt sân {} và nhả lại khung giờ do thanh toán thất bại.", bookingId);
            
            // Bắn thông báo Kafka báo thanh toán thất bại
            sendNotificationEvent(booking, "Thanh toán thất bại", "Đơn hàng " + bookingId + " đã bị hủy do thanh toán không thành công.", "PAYMENT_FAILED");
        }
    }
    
    private void sendNotificationEvent(Booking booking, String title, String content, String type) {
        try {
            // Lấy email thật từ Auth Service
            String userEmail = "";
            String fcmToken = "";
            try {
                UserDto userInfo = userClient.getUserById(booking.getUserId());
                if (userInfo != null) {
                    userEmail = userInfo.getEmail();
                    fcmToken = userInfo.getFcmToken();
                }
            } catch (Exception ex) {
                log.error("Không thể lấy thông tin User từ Auth Service", ex);
            }
            
            NotificationEventDto notificationEvent = NotificationEventDto.builder()
                    .userId(booking.getUserId())
                    .userEmail(userEmail)
                    .fcmToken(fcmToken)
                    .title(title)
                    .content(content)
                    .type(type)
                    .build();
            bookingProducerService.sendNotification(notificationEvent);
        } catch (Exception e) {
            log.error("Không thể gửi thông báo sau khi xử lý thanh toán: {}", e.getMessage());
        }
    }
}