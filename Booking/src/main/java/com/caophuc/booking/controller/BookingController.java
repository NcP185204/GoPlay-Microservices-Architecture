package com.caophuc.booking.controller;

import com.caophuc.booking.dto.*;
import com.caophuc.booking.model.PricingRule;
import com.caophuc.booking.service.BookingService;
import com.caophuc.booking.service.CourtScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CourtScheduleService courtScheduleService;

    // API Đặt sân
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @RequestBody BookingRequest request,
            @RequestHeader("X-User-Id") Integer userId
    ) {
        return ResponseEntity.ok(bookingService.createBooking(request, userId));
    }

    // API Xem lịch sử đặt sân của tôi
    @GetMapping("/my-history")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @RequestHeader("X-User-Id") Integer userId
    ) {
        return ResponseEntity.ok(bookingService.getUserBookings(userId));
    }

    // --- API MỚI: LẤY CHI TIẾT BOOKING THEO ID (Cho Payment Service gọi) ---
    // Loại bỏ yêu cầu @RequestHeader("X-User-Id") vì Payment Service gọi sang không có thông tin user, 
    // hoặc có thể cấu hình lại Payment Service để truyền header này.
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Integer bookingId) {
        Optional<BookingResponse> booking = bookingService.getBookingById(bookingId);
        return booking.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- API MỚI: HỦY ĐẶT SÂN ---
    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Integer bookingId,
            @RequestHeader("X-User-Id") Integer userId
    ) {
        bookingService.cancelBooking(bookingId, userId);
        return ResponseEntity.ok().build();
    }

    // --- API MỚI: LẤY TRẬN ĐẤU SẮP TỚI ---
    @GetMapping("/upcoming")
    public ResponseEntity<BookingResponse> getUpcomingBooking(
            @RequestHeader("X-User-Id") Integer userId
    ) {
        Optional<BookingResponse> upcomingBooking = bookingService.getUpcomingBooking(userId);

        // Nếu có trận đấu, trả về 200 OK với dữ liệu. Nếu không, trả về 204 No Content
        return upcomingBooking
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/courts/{courtId}/available-slots")
    public ResponseEntity<List<TimeSlotDto>> getAvailableSlots(
            @PathVariable Integer courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(courtScheduleService.getAvailableTimeSlots(courtId, date));
    }

    @PostMapping("/courts/{courtId}/generate-slots")
    public ResponseEntity<?> generateSlots(
            @PathVariable Integer courtId,
            @Valid @RequestBody GenerateTimeSlotRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Integer managerId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        if (managerId == null || (!"ROLE_MANAGER".equals(role) && !"ADMIN".equals(role))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Chỉ Admin/Manager mới được tạo lịch.");
        }
        List<TimeSlotDto> generatedSlots = courtScheduleService.generateInitialTimeSlots(courtId, request, managerId);
        return new ResponseEntity<>(generatedSlots, HttpStatus.CREATED);
    }

    @PostMapping("/courts/{courtId}/pricing-rules")
    public ResponseEntity<?> setPricingRule(
            @PathVariable Integer courtId,
            @Valid @RequestBody PricingRuleDto dto,
            @RequestHeader(value = "X-User-Id", required = false) Integer managerId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        if (managerId == null || (!"ROLE_MANAGER".equals(role) && !"ADMIN".equals(role))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Chỉ Admin/Manager mới được tạo luật giá.");
        }
        PricingRule newRule = courtScheduleService.setPricingRule(courtId, dto, managerId);
        return new ResponseEntity<>(newRule, HttpStatus.CREATED);
    }

    @GetMapping("/courts/{courtId}/pricing-rules")
    public ResponseEntity<List<PricingRule>> getPricingRules(@PathVariable Integer courtId) {
        return ResponseEntity.ok(courtScheduleService.getPricingRules(courtId));
    }

    @DeleteMapping("/courts/{courtId}/pricing-rules/{ruleId}")
    public ResponseEntity<?> deletePricingRule(
            @PathVariable Integer courtId,
            @PathVariable Integer ruleId,
            @RequestHeader(value = "X-User-Id", required = false) Integer managerId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        if (managerId == null || (!"ROLE_MANAGER".equals(role) && !"ADMIN".equals(role))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        courtScheduleService.deletePricingRule(courtId, ruleId, managerId);
        return ResponseEntity.noContent().build();
    }
}
