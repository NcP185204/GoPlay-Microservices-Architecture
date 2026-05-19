package com.caophuc.booking.service;

import com.caophuc.booking.dto.BookingRequest;
import com.caophuc.booking.dto.BookingResponse;

import java.util.List;
import java.util.Optional;

public interface BookingService {
    // Hàm tạo đơn đặt sân
    BookingResponse createBooking(BookingRequest request, Integer user);
    
    // Hàm xem lịch sử đặt sân
    List<BookingResponse> getUserBookings(Integer user);

    // Hàm hủy đặt sân
    void cancelBooking(Integer bookingId, Integer user);

    // Hàm lấy trận đấu sắp tới
    Optional<BookingResponse> getUpcomingBooking(Integer user);

    // Hàm lấy chi tiết một booking theo ID (dành cho service khác gọi)
    Optional<BookingResponse> getBookingById(Integer bookingId);
}
