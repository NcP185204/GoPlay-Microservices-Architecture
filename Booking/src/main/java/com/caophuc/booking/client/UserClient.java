package com.caophuc.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Tên "AUTH" (viết hoa/thường tùy vào cách bạn đăng ký, thường là viết hoa theo convention của Spring Cloud)
@FeignClient(name = "AUTH")
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserDto getUserById(@PathVariable("id") Integer id);
}