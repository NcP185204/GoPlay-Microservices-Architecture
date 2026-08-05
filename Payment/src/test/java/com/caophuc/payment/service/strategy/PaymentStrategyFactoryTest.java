package com.caophuc.payment.service.strategy;

import com.caophuc.payment.exception.PaymentMethodNotFoundException;
import com.caophuc.payment.service.PaymentStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentStrategyFactoryTest {

    private PaymentStrategyFactory paymentStrategyFactory;

    @Mock
    private MomoPaymentStrategy momoPaymentStrategy;
    @Mock
    private CashPaymentStrategy cashPaymentStrategy;

    @BeforeEach
    void setUp() {
        when(momoPaymentStrategy.getPaymentMethodName()).thenReturn("MOMO");
        when(cashPaymentStrategy.getPaymentMethodName()).thenReturn("CASH");

        paymentStrategyFactory = new PaymentStrategyFactory(List.of(momoPaymentStrategy, cashPaymentStrategy));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MOMO", "momo", "MoMo"})
    @DisplayName("Should return MomoPaymentStrategy for various 'MOMO' casings")
    void whenGetStrategy_withMomoMethod_shouldReturnMomoPaymentStrategy(String method) {
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(method);
        assertSame(momoPaymentStrategy, strategy);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CASH", "cash"})
    @DisplayName("Should return CashPaymentStrategy for various 'CASH' casings")
    void whenGetStrategy_withCashMethod_shouldReturnCashPaymentStrategy(String method) {
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(method);
        assertSame(cashPaymentStrategy, strategy);
    }

    @Test
    @DisplayName("Should throw PaymentMethodNotFoundException for an unknown method")
    void whenGetStrategy_withUnknownMethod_shouldThrowException() {
        String method = "ZALOPAY";

        Exception exception = assertThrows(PaymentMethodNotFoundException.class, () -> {
            paymentStrategyFactory.getStrategy(method);
        });
        assertTrue(exception.getMessage().contains("not supported"));
    }
}
