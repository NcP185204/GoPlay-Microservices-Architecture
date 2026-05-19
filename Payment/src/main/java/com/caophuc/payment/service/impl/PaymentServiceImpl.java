package com.caophuc.payment.service.impl;

import com.caophuc.payment.client.BookingClient;
import com.caophuc.payment.client.BookingDto;
import com.caophuc.payment.dto.MomoIpnRequest;
import com.caophuc.payment.dto.PaymentEvent;
import com.caophuc.payment.dto.PaymentResponse;
import com.caophuc.payment.exception.ResourceNotFoundException;
import com.caophuc.payment.model.Payment;
import com.caophuc.payment.repository.PaymentRepository;
import com.caophuc.payment.service.MomoSecurityService;
import com.caophuc.payment.service.PaymentService;
import com.caophuc.payment.service.strategy.PaymentStrategy;
import com.caophuc.payment.service.strategy.PaymentStrategyFactory;
import com.caophuc.payment.util.PaymentStatus;
// ... (Import các class Strategy của bạn) ...
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentStrategyFactory paymentStrategyFactory;
    private final MomoSecurityService momoSecurityService;
    private final BookingClient bookingClient;
    
    // Thêm KafkaTemplate để gửi message
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    // Tên topic để gửi event
    private static final String PAYMENT_EVENT_TOPIC = "payment-events-topic";

    @Override
    @Transactional
    public PaymentResponse createPayment(Integer bookingId, Integer userId) {
        // 1. Dùng FeignClient gọi sang Booking Service lấy thông tin (Đã bao gồm check quyền)
        BookingDto booking = getBookingAndCheckOwnership(bookingId, userId);

        if (!"PENDING".equals(booking.getStatus())) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái chờ thanh toán.");
        }

        // 2. Tìm hoặc tạo mới bản ghi Payment trong Database của Payment Service
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseGet(() -> createInitialPayment(booking, userId));

        // 3. Tạo link thanh toán qua MoMo/VNPay...
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(payment.getPaymentMethod());
        String transactionId = "MOMO_" + UUID.randomUUID().toString();
        PaymentResponse paymentResponse = strategy.createPaymentRequest(transactionId, payment.getAmount());

        // 4. Lưu lại transactionId
        payment.setTransactionId(transactionId);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return paymentResponse;
    }

    @Override
    @Transactional
    public void handleMomoWebhook(MomoIpnRequest payload) {
        try {
            // 1. Kiểm tra tính toàn vẹn của dữ liệu (Signature Validation)
            if (!momoSecurityService.validateSignature(payload)) {
                log.error(" Chữ ký không khớp! Có thể là request giả mạo.");
                return; // Có thể throw exception nếu muốn
            }

            log.info("Chữ ký hợp lệ. Request chính chủ từ MoMo.");
            String transactionId = payload.getOrderId();

            // 2. Xử lý nghiệp vụ dựa trên ResultCode
            if (payload.getResultCode() == 0) {
                log.info("Thanh toán THÀNH CÔNG cho mã: {}", transactionId);
                handleSuccessfulPayment(transactionId);
            } else {
                log.warn(" Thanh toán THẤT BẠI. Lỗi: {}", payload.getMessage());
                handleFailedPayment(transactionId, payload.getMessage());
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý Webhook MoMo: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi xử lý webhook", e);
        }
    }

    @Override
    @Transactional
    public void handleSuccessfulPayment(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch."));

        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Tạo Event object
            PaymentEvent event = PaymentEvent.builder()
                    .bookingId(payment.getBookingId())
                    .status("SUCCESS")
                    .message("Thanh toán thành công")
                    .build();

            // Gửi event lên Kafka
            kafkaTemplate.send(PAYMENT_EVENT_TOPIC, String.valueOf(payment.getBookingId()), event);
            log.info("[KAFKA] Đã gửi sự kiện: Thanh toán THÀNH CÔNG cho Booking ID: {} vào topic: {}", payment.getBookingId(), PAYMENT_EVENT_TOPIC);
        }
    }

    @Override
    @Transactional
    public void handleFailedPayment(String transactionId, String errorMessage) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch."));

        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.FAILED);
            // Lưu thêm errorMessage vào DB nếu model của bạn có trường này
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Tạo Event object
            PaymentEvent event = PaymentEvent.builder()
                    .bookingId(payment.getBookingId())
                    .status("FAILED")
                    .message(errorMessage != null ? errorMessage : "Thanh toán thất bại")
                    .build();

            // Gửi event lên Kafka
            kafkaTemplate.send(PAYMENT_EVENT_TOPIC, String.valueOf(payment.getBookingId()), event);
            log.info(" [KAFKA] Đã gửi sự kiện: Thanh toán THẤT BẠI cho Booking ID: {} vào topic: {}", payment.getBookingId(), PAYMENT_EVENT_TOPIC);
        }
    }

    @Override
    public PaymentResponse getPaymentInfo(Integer bookingId, Integer userId) {
        getBookingAndCheckOwnership(bookingId, userId); // Check quyền xem

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán cho đơn hàng này."));

        if (payment.getTransactionId() == null) {
            return PaymentResponse.builder()
                    .message("Chưa có link thanh toán cho đơn hàng này.")
                    .build();
        }

        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(payment.getPaymentMethod());
        return strategy.createPaymentRequest(payment.getTransactionId(), payment.getAmount());
    }

    // --- CÁC HÀM PRIVATE HỖ TRỢ ---

    private BookingDto getBookingAndCheckOwnership(Integer bookingId, Integer userId) {
        try {
            // Nhấc máy gọi FeignClient sang Booking Service
            BookingDto booking = bookingClient.getBookingById(bookingId);

            if (booking == null) {
                throw new ResourceNotFoundException("Không tìm thấy đơn hàng");
            }
            if (!Objects.equals(booking.getUserId(), userId)) {
                throw new RuntimeException("Bạn không có quyền xem hoặc thao tác trên đơn hàng này.");
            }
            return booking;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi kết nối với Booking Service: " + e.getMessage());
        }
    }

    private Payment createInitialPayment(BookingDto booking, Integer userId) {
        return Payment.builder()
                .bookingId(booking.getId()) // Lưu ID thay vì Object
                .userId(userId)
                .amount(booking.getTotalPrice())
                .status(PaymentStatus.PENDING)
                .paymentMethod(booking.getPaymentMethod() != null ? booking.getPaymentMethod() : "MOMO")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
