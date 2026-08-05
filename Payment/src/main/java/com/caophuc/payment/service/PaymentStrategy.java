package com.caophuc.payment.service;

import com.caophuc.payment.dto.PaymentResponse;


public interface PaymentStrategy {
    String getPaymentMethodName();
    PaymentResponse createPaymentRequest(String orderId, Double amount);
}
