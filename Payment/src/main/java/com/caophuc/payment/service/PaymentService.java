package com.caophuc.payment.service;

import com.caophuc.payment.dto.MomoIpnRequest;
import com.caophuc.payment.dto.PaymentResponse;


public interface PaymentService {
    // Tạo link thanh toán cho một đơn hàng
    PaymentResponse createPayment(Integer bookingId, Integer userId);

    // Xử lý webhook từ MoMo
    void handleMomoWebhook(MomoIpnRequest payload);

    // Xử lý khi thanh toán thành công (Momo gọi về webhook)
    void handleSuccessfulPayment(String transactionId);

    // Xử lý khi thanh toán thất bại/hủy (Momo gọi về webhook)
    void handleFailedPayment(String transactionId, String errorMessage);

    // Lấy thông tin thanh toán hiện tại
    PaymentResponse getPaymentInfo(Integer bookingId, Integer userId);
}
