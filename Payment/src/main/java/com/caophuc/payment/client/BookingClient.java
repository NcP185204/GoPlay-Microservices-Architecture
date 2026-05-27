package com.caophuc.payment.client;

import com.caophuc.payment.config.FeignClientInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Tên này phải trùng với spring.application.name của Booking Service trên Eureka
@FeignClient(name = "BOOKING",configuration = FeignClientInterceptor.class)
public interface BookingClient {

    // Đường dẫn này gọi sang API nội bộ của Booking Service
    @GetMapping("/api/bookings/{id}")
    BookingDto getBookingById(@PathVariable("id") Integer id);
}
