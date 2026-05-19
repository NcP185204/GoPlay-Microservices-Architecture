package com.caophuc.auth.service.strategy;


import com.caophuc.auth.dto.SocialUserInfo;

public interface SocialAuthStrategy {
    /**
     * Trả về tên của provider mà strategy này hỗ trợ (ví dụ: "GOOGLE", "FACEBOOK").
     */
    String getProviderName();

    /**
     * Xác thực token và trả về thông tin người dùng.
     */
    SocialUserInfo verifyToken(String token);
}
