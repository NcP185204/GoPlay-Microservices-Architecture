package com.caophuc.payment.service;

import com.caophuc.payment.dto.MomoIpnRequest;

public interface MomoSecurityService {
    /**
     * Hàm kiểm tra tính hợp lệ của chữ ký do MoMo gửi về
     */
    boolean validateSignature(MomoIpnRequest payload) throws Exception;
}
