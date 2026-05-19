package com.caophuc.auth.dto;

import lombok.Data;

@Data
public class SocialLoginRequest {
    // ID Token từ Google HOẶC Access Token từ Facebook
    private String token;

    // Chỉ rõ nhà cung cấp là ai (GOOGLE, FACEBOOK)
    private String provider;
}
