package com.caophuc.auth.controller;


import com.caophuc.auth.dto.*;
import com.caophuc.auth.exception.TokenRefreshException;
import com.caophuc.auth.model.RefreshToken;
import com.caophuc.auth.security.JwtTokenProvider;
import com.caophuc.auth.service.AuthService;
import com.caophuc.auth.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/social-login")
    public ResponseEntity<AuthResponse> socialLogin(@RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(authService.socialLogin(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtTokenProvider.generateAccessToken(user);
                    return ResponseEntity.ok(new TokenRefreshResponse(token, requestRefreshToken));
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token is not in database!"));
    }

    @PostMapping("/logout")
    // Do Auth Service không còn tự chặn bằng Token nữa, ta lấy email user do Gateway truyền xuống qua Header
    public ResponseEntity<?> logoutUser(@RequestHeader("X-User-Email") String userEmail) {
        // Trong dự án thực tế, bạn có thể thêm logic lấy User từ DB thông qua userEmail để xóa refresh token,
        // hoặc viết thêm 1 hàm trong RefreshTokenService: deleteByUserEmail(String email)
        
        // refreshTokenService.deleteByUserEmail(userEmail); 
        return ResponseEntity.ok("Log out successful for user: " + userEmail);
    }
}
