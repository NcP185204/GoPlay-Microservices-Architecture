package com.caophuc.payment.service.strategy;

import com.caophuc.payment.config.MomoConfig;
import com.caophuc.payment.dto.MomoCreatePaymentResponse;
import com.caophuc.payment.dto.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MomoPaymentStrategyTest {

    @Mock
    private MomoConfig momoConfig;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MomoPaymentStrategy momoPaymentStrategy;

    @BeforeEach
    void setUp() {
        // Dạy cho MomoConfig trả về các giá trị giả lập cần thiết
        when(momoConfig.getPartnerCode()).thenReturn("FAKE_PARTNER_CODE");
        when(momoConfig.getAccessKey()).thenReturn("FAKE_ACCESS_KEY");
        when(momoConfig.getSecretKey()).thenReturn("FAKE_SECRET_KEY");
        when(momoConfig.getEndpointUrl()).thenReturn("https://fake-momo-endpoint.com");
        when(momoConfig.getIpnUrl()).thenReturn("https://fake-ipn-url.com");
        when(momoConfig.getRedirectUrl()).thenReturn("https://fake-redirect-url.com");
    }

    @Test
    @DisplayName("Should return PaymentResponse with URL when Momo API call is successful")
    void whenCreatePaymentRequest_andMomoApiSucceeds_shouldReturnPaymentUrl() {
        // 1. ARRANGE
        String transactionId = "MOMO_12345";
        double amount = 250000.0;

        // a. Tạo một phản hồi giả lập thành công từ MoMo
        MomoCreatePaymentResponse fakeMomoResponse = new MomoCreatePaymentResponse();
        fakeMomoResponse.setResultCode(0);
        fakeMomoResponse.setPayUrl("https://momo.vn/pay/xyz");

        // b. Dạy cho RestTemplate trả về phản hồi giả lập này khi được gọi
        when(restTemplate.postForEntity(
                eq(momoConfig.getEndpointUrl()), // Đảm bảo gọi đúng endpoint
                any(HttpEntity.class),          // Với bất kỳ body nào
                eq(MomoCreatePaymentResponse.class) // Và mong đợi kiểu trả về này
        )).thenReturn(new ResponseEntity<>(fakeMomoResponse, HttpStatus.OK));

        // 2. ACT
        PaymentResponse actualResponse = momoPaymentStrategy.createPaymentRequest(transactionId, amount);

        // 3. ASSERT
        assertNotNull(actualResponse);
        assertEquals(fakeMomoResponse.getPayUrl(), actualResponse.getPaymentUrl());
        assertTrue(actualResponse.getMessage().contains("thành công"));
    }

    @Test
    @DisplayName("Should throw RuntimeException when Momo API returns an error")
    void whenCreatePaymentRequest_andMomoApiFails_shouldThrowRuntimeException() {
        // 1. ARRANGE
        String transactionId = "MOMO_67890";
        double amount = 300000.0;

        // a. Tạo một phản hồi giả lập thất bại từ MoMo
        MomoCreatePaymentResponse fakeMomoResponse = new MomoCreatePaymentResponse();
        fakeMomoResponse.setResultCode(1001); // Mã lỗi từ MoMo
        fakeMomoResponse.setMessage("Invalid signature");

        // b. Dạy cho RestTemplate trả về phản hồi lỗi này
        when(restTemplate.postForEntity(
                any(String.class),
                any(HttpEntity.class),
                eq(MomoCreatePaymentResponse.class)
        )).thenReturn(new ResponseEntity<>(fakeMomoResponse, HttpStatus.OK));

        // 2. ACT & 3. ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            momoPaymentStrategy.createPaymentRequest(transactionId, amount);
        });

        assertTrue(exception.getMessage().contains("Lỗi từ MoMo: Invalid signature"));
    }
}
