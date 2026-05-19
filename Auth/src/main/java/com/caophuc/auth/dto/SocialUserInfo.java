package com.caophuc.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SocialUserInfo {
    private String email;
    private String name;
    private String provider;
}