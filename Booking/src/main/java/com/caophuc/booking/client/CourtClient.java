package com.caophuc.booking.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Tên "COURT" phải khớp với spring.application.name của Court Service trên Eureka
@FeignClient(name = "COURT")
public interface CourtClient {

    // Khai báo đường dẫn API mà Court Service đang có để lấy chi tiết 1 sân
    @GetMapping("/api/courts/{id}")
    CourtDto getCourtById(@PathVariable("id") Integer id);
}