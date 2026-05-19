package com.caophuc.auth.dto;

import com.caophuc.auth.util.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    private Integer id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private UserRole role;
    
    // Thuộc tính phục vụ cho Push Notification
    private String fcmToken; 
}
