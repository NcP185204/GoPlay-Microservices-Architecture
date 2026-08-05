package com.caophuc.payment.service.strategy;

import com.caophuc.payment.dto.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CashPaymentStrategyTest {

    private CashPaymentStrategy cashPaymentStrategy;

    @BeforeEach
    void setUp() {
        cashPaymentStrategy = new CashPaymentStrategy();
    }

    @Test
    @DisplayName("Should return a response with a confirmation message and no URL")
    void whenCreatePaymentRequest_shouldReturnConfirmationMessage() {
        // ARRANGE
        String transactionId = "CASH_123";
        double amount = 150000.0;

        // ACT
        PaymentResponse response = cashPaymentStrategy.createPaymentRequest(transactionId, amount);

        // ASSERT
        assertNotNull(response);
        assertNull(response.getPaymentUrl(), "Cash payment should not have a payment URL");
        assertNotNull(response.getMessage());
        // SỬA LẠI ĐÂY: Kiểm tra chuỗi message cho khớp với logic thật
        assertTrue(response.getMessage().contains("Đơn hàng sẽ được thanh toán bằng tiền mặt tại sân."), "Message should confirm cash payment at the location");
    }
}
