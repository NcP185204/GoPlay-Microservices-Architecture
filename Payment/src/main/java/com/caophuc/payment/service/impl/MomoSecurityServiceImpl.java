package com.caophuc.payment.service.impl;

import com.caophuc.payment.dto.MomoIpnRequest;
import com.caophuc.payment.service.MomoSecurityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
public class MomoSecurityServiceImpl implements MomoSecurityService {

    @Value("${momo.access-key}")
    private String accessKey;

    @Value("${momo.secret-key}")
    private String secretKey;

    @Override
    public boolean validateSignature(MomoIpnRequest payload) throws Exception {
        // Nối chuỗi theo đúng tài liệu của MoMo
        String rawHash = "accessKey=" + accessKey +
                "&amount=" + payload.getAmount() +
                "&extraData=" + payload.getExtraData() +
                "&message=" + payload.getMessage() +
                "&orderId=" + payload.getOrderId() +
                "&orderInfo=" + payload.getOrderInfo() +
                "&orderType=" + payload.getOrderType() +
                "&partnerCode=" + payload.getPartnerCode() +
                "&payType=" + payload.getPayType() +
                "&requestId=" + payload.getRequestId() +
                "&responseTime=" + payload.getResponseTime() +
                "&resultCode=" + payload.getResultCode() +
                "&transId=" + payload.getTransId();

        String expectedSignature = hmacSHA256(rawHash, secretKey);

        // So sánh chữ ký tính toán được với chữ ký MoMo gửi sang
        return expectedSignature.equals(payload.getSignature());
    }

    /**
     * Hàm mã hóa thuật toán HMAC SHA256
     */
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
