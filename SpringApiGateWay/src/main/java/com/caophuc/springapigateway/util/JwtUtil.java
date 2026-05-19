package com.caophuc.springapigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    // Khóa bí mật lấy từ application.yml, phải khớp với khóa được dùng ở Auth-Service
    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * Hàm dùng để kiểm tra tính hợp lệ của Token
     * Nếu token không hợp lệ (hết hạn, sai chữ ký), JJWT sẽ quăng Exception tương ứng
     */
    public void validateToken(final String token) {
        Jwts.parser().verifyWith(getSignKey()).build().parseSignedClaims(token);
    }

    /**
     * Lấy toàn bộ Payload (Claims) ra khỏi Token
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Tạo SecretKey object từ chuỗi base64 đã cấu hình
     */
    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}