package com.caophuc.payment.service.impl;

import com.caophuc.payment.dto.MomoIpnRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;

import static org.junit.jupiter.api.Assertions.*;

class MomoSecurityServiceImplTest {

    private MomoSecurityServiceImpl momoSecurityService;

    private final String FAKE_SECRET_KEY = "thisisafakesecretkeyfortesting12345";
    private final String FAKE_ACCESS_KEY = "FAKEACCESSKEY";

    @BeforeEach
    void setUp() {
        momoSecurityService = new MomoSecurityServiceImpl();
        ReflectionTestUtils.setField(momoSecurityService, "secretKey", FAKE_SECRET_KEY);
        ReflectionTestUtils.setField(momoSecurityService, "accessKey", FAKE_ACCESS_KEY);
    }

    @Test
    @DisplayName("Should return true when the signature is valid")
    void whenSignatureIsValid_shouldReturnTrue() throws Exception {
        MomoIpnRequest request = createSampleMomoRequest();
        String correctSignature = generateCorrectSignature(request);
        request.setSignature(correctSignature);

        boolean isValid = momoSecurityService.validateSignature(request);

        assertTrue(isValid, "Signature should be valid");
    }

    @Test
    @DisplayName("Should return false when the signature is incorrect")
    void whenSignatureIsInvalid_shouldReturnFalse() throws Exception {
        MomoIpnRequest request = createSampleMomoRequest();
        request.setSignature("thisisawrongsignature");

        boolean isValid = momoSecurityService.validateSignature(request);

        assertFalse(isValid, "An incorrect signature should be invalid");
    }

    @Test
    @DisplayName("Should return false when data is tampered with (e.g., amount changed)")
    void whenDataIsTampered_shouldReturnFalse() throws Exception {
        MomoIpnRequest request = createSampleMomoRequest();
        String correctSignatureForOriginalData = generateCorrectSignature(request);
        request.setSignature(correctSignatureForOriginalData);

        request.setAmount(1000L);

        boolean isValid = momoSecurityService.validateSignature(request);

        assertFalse(isValid, "Signature should be invalid if data is tampered with");
    }

    private String generateCorrectSignature(MomoIpnRequest request) {
        String rawSignatureString = "accessKey=" + FAKE_ACCESS_KEY +
                "&amount=" + request.getAmount() +
                "&extraData=" + request.getExtraData() +
                "&message=" + request.getMessage() +
                "&orderId=" + request.getOrderId() +
                "&orderInfo=" + request.getOrderInfo() +
                "&orderType=" + request.getOrderType() +
                "&partnerCode=" + request.getPartnerCode() +
                "&payType=" + request.getPayType() +
                "&requestId=" + request.getRequestId() +
                "&responseTime=" + request.getResponseTime() +
                "&resultCode=" + request.getResultCode() +
                "&transId=" + request.getTransId();
        return generateHmacSha256(rawSignatureString, FAKE_SECRET_KEY);
    }

    private MomoIpnRequest createSampleMomoRequest() {
        MomoIpnRequest request = new MomoIpnRequest();
        request.setPartnerCode("MOMOBKUN20180529");
        request.setOrderId("GOPLAY_12345");
        request.setRequestId("GOPLAY_12345");
        request.setAmount(50000L);
        request.setOrderInfo("Thanh toan don hang GoPlay");
        request.setOrderType("momo_wallet");
        request.setTransId(2369110321L);
        request.setResultCode(0);
        request.setMessage("Success");
        request.setPayType("qr");
        request.setResponseTime(1593058555944L);
        request.setExtraData("");
        return request;
    }

    private String generateHmacSha256(String data, String key) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            Formatter formatter = new Formatter();
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC-SHA256", e);
        }
    }
}
