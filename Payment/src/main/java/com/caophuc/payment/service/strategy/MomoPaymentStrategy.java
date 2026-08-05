package com.caophuc.payment.service.strategy;

import com.caophuc.payment.config.MomoConfig;
import com.caophuc.payment.dto.MomoCreatePaymentRequest;
import com.caophuc.payment.dto.MomoCreatePaymentResponse;
import com.caophuc.payment.dto.PaymentResponse;
import com.caophuc.payment.service.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor // Lombok sẽ tự tạo constructor cho các trường final
public class MomoPaymentStrategy implements PaymentStrategy {

    // Inject các dependency qua constructor
    private final MomoConfig momoConfig;
    private final RestTemplate restTemplate;

    @Override
    public String getPaymentMethodName() {
        return "MOMO";
    }

    @Override
    public PaymentResponse createPaymentRequest(String orderId, Double amount) {
        try {
            Long amountLong = amount.longValue();
            String amountStr = String.valueOf(amountLong);
            String requestId = UUID.randomUUID().toString();
            String orderInfo = "Thanh toán đặt sân GoPlay";
            String requestType = "captureWallet";
            String extraData = "";

            // Lấy giá trị từ momoConfig thay vì @Value
            String rawHash = "accessKey=" + momoConfig.getAccessKey() +
                    "&amount=" + amountStr +
                    "&extraData=" + extraData +
                    "&ipnUrl=" + momoConfig.getIpnUrl() +
                    "&orderId=" + orderId +
                    "&orderInfo=" + orderInfo +
                    "&partnerCode=" + momoConfig.getPartnerCode() +
                    "&redirectUrl=" + momoConfig.getRedirectUrl() +
                    "&requestId=" + requestId +
                    "&requestType=" + requestType;

            String signature = hmacSHA256(rawHash, momoConfig.getSecretKey());

            MomoCreatePaymentRequest requestBody = MomoCreatePaymentRequest.builder()
                    .partnerCode(momoConfig.getPartnerCode())
                    .partnerName("Test")
                    .storeId("MomoTestStore")
                    .requestType(requestType)
                    .ipnUrl(momoConfig.getIpnUrl())
                    .redirectUrl(momoConfig.getRedirectUrl())
                    .orderId(orderId)
                    .amount(amountLong)
                    .lang("vi")
                    .orderInfo(orderInfo)
                    .requestId(requestId)
                    .extraData(extraData)
                    .signature(signature)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<MomoCreatePaymentRequest> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<MomoCreatePaymentResponse> response = restTemplate.postForEntity(
                    momoConfig.getEndpointUrl(),
                    entity,
                    MomoCreatePaymentResponse.class
            );

            MomoCreatePaymentResponse responseBody = response.getBody();

            if (responseBody != null && responseBody.getResultCode() != null) {
                if (responseBody.getResultCode() == 0) {
                    return PaymentResponse.builder()
                            .paymentUrl(responseBody.getPayUrl())
                            .message("Tạo link thanh toán Momo thành công.")
                            .build();
                } else {
                    throw new RuntimeException("Lỗi từ MoMo: " + responseBody.getMessage());
                }
            }
            throw new RuntimeException("Không nhận được phản hồi hợp lệ từ MoMo");

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo yêu cầu thanh toán MoMo: " + e.getMessage(), e);
        }
    }

    private String hmacSHA256(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
