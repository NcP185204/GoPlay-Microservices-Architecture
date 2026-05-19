package com.caophuc.payment.controller;

import com.caophuc.payment.dto.MomoIpnRequest; 
import com.caophuc.payment.dto.PaymentResponse; 
import com.caophuc.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/bookings/{bookingId}/create-payment")
    public ResponseEntity<PaymentResponse> createPayment(
            @PathVariable Integer bookingId,
            @RequestHeader("X-User-Id") Integer userId) {

        return ResponseEntity.ok(paymentService.createPayment(bookingId, userId));
    }

    @GetMapping("/bookings/{bookingId}/payment-info")
    public ResponseEntity<PaymentResponse> getPaymentInfo(
            @PathVariable Integer bookingId,
            @RequestHeader("X-User-Id") Integer userId) {

        return ResponseEntity.ok(paymentService.getPaymentInfo(bookingId, userId));
    }

    // API Webhook - MoMo gọi vào đây qua Ngrok (API này phải được PUBLIC ở Gateway)
    @PostMapping("/payments/momo/success")
    public ResponseEntity<Void> momoSuccessWebhook(@RequestBody MomoIpnRequest payload) {
        log.info("\n--- NHẬN ĐƯỢC WEBHOOK TỪ MOMO ---");
        log.info("OrderId: {}", payload.getOrderId());
        log.info("ResultCode: {}", payload.getResultCode());

        // Đẩy toàn bộ logic xử lý webhook xuống Service
        paymentService.handleMomoWebhook(payload);

        return ResponseEntity.ok().build();
    }
}
