package com.caophuc.auth.controller;

import com.caophuc.auth.dto.UserProfileResponse;
import com.caophuc.auth.exception.ResourceNotFoundException;
import com.caophuc.auth.model.User;
import com.caophuc.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // API 1: Dành cho các Microservices khác (như Booking, Payment) gọi để lấy thông tin User qua ID
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable Integer id) {
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        UserProfileResponse response = UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .fcmToken(user.getFcmToken())
                .build();

        return ResponseEntity.ok(response);
    }

//    // API 2: Dành cho App Android/Web gọi để lấy thông tin của chính mình đang đăng nhập
//    @GetMapping("/me")
//    public ResponseEntity<UserProfileResponse> getMyProfile(@RequestHeader("X-User-Email") String userEmail) {
//
//        // Tìm User dựa vào email do API Gateway bóc tách từ Token và gắn vào Header
//        User user = userRepository.findByEmail(userEmail)
//                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
//
//        UserProfileResponse response = UserProfileResponse.builder()
//                .id(user.getId())
//                .fullName(user.getFullName())
//                .email(user.getEmail())
//                .phoneNumber(user.getPhoneNumber())
//                .role(user.getRole())
//                .fcmToken(user.getFcmToken())
//                .build();
//
//        return ResponseEntity.ok(response);
//    }
}
