package com.caophuc.booking.client;

import lombok.Data;

@Data
public class UserDto {
    private Integer id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String role; // Giả sử role là String, ví dụ: "ROLE_USER"
    private String fcmToken;
}