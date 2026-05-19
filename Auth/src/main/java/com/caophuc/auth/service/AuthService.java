package com.caophuc.auth.service;


import com.caophuc.auth.dto.AuthResponse;
import com.caophuc.auth.dto.LoginRequest;
import com.caophuc.auth.dto.RegisterRequest;
import com.caophuc.auth.dto.SocialLoginRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse socialLogin(SocialLoginRequest request);
}
