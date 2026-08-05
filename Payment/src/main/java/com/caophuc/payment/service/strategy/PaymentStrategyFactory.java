package com.caophuc.payment.service.strategy;

import com.caophuc.payment.exception.PaymentMethodNotFoundException;
import com.caophuc.payment.service.PaymentStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentStrategyFactory {

    private final Map<String, PaymentStrategy> strategies;

    public PaymentStrategyFactory(List<PaymentStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(s -> s.getPaymentMethodName().toUpperCase(), Function.identity()));
    }

    public PaymentStrategy getStrategy(String method) {
        PaymentStrategy strategy = strategies.get(method.toUpperCase());
        if (strategy == null) {
            // SỬA LẠI ĐÂY: Ném ra exception tùy chỉnh của chúng ta
            throw new PaymentMethodNotFoundException("Payment method '" + method + "' not supported");
        }
        return strategy;
    }
}
