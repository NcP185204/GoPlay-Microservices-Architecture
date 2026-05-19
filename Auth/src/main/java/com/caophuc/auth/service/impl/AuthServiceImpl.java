package com.caophuc.auth.service.impl;


import com.caophuc.auth.dto.*;
import com.caophuc.auth.model.RefreshToken;
import com.caophuc.auth.model.User;
import com.caophuc.auth.repository.UserRepository;
import com.caophuc.auth.security.JwtTokenProvider;
import com.caophuc.auth.service.AuthService;
import com.caophuc.auth.service.RefreshTokenService;
import com.caophuc.auth.service.strategy.SocialAuthStrategy;
import com.caophuc.auth.service.strategy.SocialAuthStrategyFactory;
import com.caophuc.auth.util.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final SocialAuthStrategyFactory socialAuthStrategyFactory;
    
    // Inject Spring ConversionService
    private final ConversionService conversionService; 

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }
        
        // 1. Dùng ConversionService chuyển từ DTO (RegisterRequest) -> Entity (User)
        User user = conversionService.convert(request, User.class);
        
        // 2. Ghi đè password đã mã hóa
        user.setPassword(passwordEncoder.encode(request.getPassword())); 
        
        User savedUser = userRepository.save(user);

        return generateAuthResponseForUser(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = (User) authentication.getPrincipal();

        return generateAuthResponseForUser(user);
    }

    @Override
    @Transactional
    public AuthResponse socialLogin(SocialLoginRequest request) {
        // Lấy đúng strategy từ factory
        SocialAuthStrategy strategy = socialAuthStrategyFactory.getStrategy(request.getProvider());
        // Thực thi strategy
        SocialUserInfo socialUserInfo = strategy.verifyToken(request.getToken());

        User user = userRepository.findByEmail(socialUserInfo.getEmail()).orElseGet(() -> {
            User newUser = User.builder()
                    .email(socialUserInfo.getEmail())
                    .fullName(socialUserInfo.getName())
                    .password(passwordEncoder.encode("SOCIAL_USER_PASSWORD"))
                    .role(UserRole.PLAYER)
                    .build();
            return userRepository.save(newUser);
        });

        return generateAuthResponseForUser(user);
    }

    private AuthResponse generateAuthResponseForUser(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        // 3. Dùng ConversionService chuyển từ Entity (User) -> DTO (AuthResponse)
        AuthResponse response = conversionService.convert(user, AuthResponse.class);
        
        // Gắn thêm các token
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken.getToken());
        
        return response;
    }
}
