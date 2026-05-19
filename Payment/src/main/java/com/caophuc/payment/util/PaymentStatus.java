package com.caophuc.payment.util;

public enum PaymentStatus {
    PENDING,    // Đang chờ thanh toán
    SUCCESS,    // Thanh toán thành công
    FAILED,     // Thanh toán thất bại
    EXPIRED     // Hết hạn thanh toán
}
