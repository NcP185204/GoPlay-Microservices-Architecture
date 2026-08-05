package com.caophuc.booking;
import com.caophuc.booking.client.CourtClient;
import com.caophuc.booking.client.CourtDto;
import com.caophuc.booking.dto.GenerateTimeSlotRequest;
import com.caophuc.booking.dto.TimeSlotDto;
import com.caophuc.booking.exception.AccessDeniedException;
import com.caophuc.booking.exception.ResourceNotFoundException;
import com.caophuc.booking.model.PricingRule;
import com.caophuc.booking.model.TimeSlot;
import com.caophuc.booking.repository.PricingRuleRepository;
import com.caophuc.booking.repository.TimeSlotRepository;
import com.caophuc.booking.service.impl.CourtScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourtScheduleServiceImplTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private CourtClient courtClient;

    @InjectMocks
    private CourtScheduleServiceImpl courtScheduleService;

    private CourtDto courtDto;
    private GenerateTimeSlotRequest request;
    private final Integer managerId = 1;
    private final Integer courtId = 1;

    @BeforeEach
    void setUp() {
        // Dữ liệu mẫu cho sân, chủ sân là managerId = 1
        courtDto = new CourtDto();
        courtDto.setId(courtId);
        courtDto.setOwnerId(managerId);
        courtDto.setPricePerHour(100.0); // Giá mặc định

        // Yêu cầu tạo lịch mẫu
        request = new GenerateTimeSlotRequest();
        request.setStartDate(LocalDate.of(2024, 1, 1));
        request.setNumberOfDays(1);
        request.setOpenTime(LocalTime.of(8, 0));
        request.setCloseTime(LocalTime.of(10, 0));
        request.setSlotDurationInMinutes(60);
    }

    @Test
    void generateInitialTimeSlots_Success_CreatesSlotsWithDefaultPrice() {
        // 1. ARRANGE
        // Giả lập: courtClient trả về thông tin sân hợp lệ
        when(courtClient.getCourtById(courtId)).thenReturn(courtDto);
        // Giả lập: Không có slot nào tồn tại từ trước
        when(timeSlotRepository.existsByCourtIdAndStartTime(anyInt(), any(LocalDateTime.class))).thenReturn(false);
        // Giả lập: Không có luật giá nào được áp dụng
        when(pricingRuleRepository.findApplicableRule(anyInt(), any(DayOfWeek.class), any(LocalTime.class)))
                .thenReturn(Collections.emptyList());
        // Giả lập: hàm saveAll sẽ trả về chính danh sách đầu vào của nó
        when(timeSlotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. ACT
        List<TimeSlotDto> result = courtScheduleService.generateInitialTimeSlots(courtId, request, managerId);

        // 3. ASSERT
        assertEquals(2, result.size()); // Phải tạo ra 2 slot: 8-9h và 9-10h
        assertEquals(100.0, result.get(0).getPrice()); // Giá phải là giá mặc định của sân
        assertEquals(100.0, result.get(1).getPrice());

        // Kiểm tra xem saveAll có được gọi với 2 slot không
        ArgumentCaptor<List<TimeSlot>> captor = ArgumentCaptor.forClass(List.class);
        verify(timeSlotRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    void generateInitialTimeSlots_Success_AppliesPricingRule() {
        // 1. ARRANGE
        // Tạo một luật giá cho "giờ vàng" từ 8-9h
        PricingRule specialPriceRule = PricingRule.builder()
                .price(250.0) // Giá giờ vàng
                .dayOfWeek(DayOfWeek.MONDAY) // Ngày 2024-01-01 là thứ Hai
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .build();

        when(courtClient.getCourtById(courtId)).thenReturn(courtDto);
        when(timeSlotRepository.existsByCourtIdAndStartTime(anyInt(), any(LocalDateTime.class))).thenReturn(false);

        // Giả lập: Khi hỏi giá cho slot 8h, trả về luật giá giờ vàng
        LocalDateTime slot8am = LocalDateTime.of(2024, 1, 1, 8, 0);
        when(pricingRuleRepository.findApplicableRule(courtId, DayOfWeek.MONDAY, slot8am.toLocalTime()))
                .thenReturn(List.of(specialPriceRule));

        // Giả lập: Khi hỏi giá cho slot 9h, không có luật nào
        LocalDateTime slot9am = LocalDateTime.of(2024, 1, 1, 9, 0);
        when(pricingRuleRepository.findApplicableRule(courtId, DayOfWeek.MONDAY, slot9am.toLocalTime()))
                .thenReturn(Collections.emptyList());

        when(timeSlotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. ACT
        List<TimeSlotDto> result = courtScheduleService.generateInitialTimeSlots(courtId, request, managerId);

        // 3. ASSERT
        assertEquals(2, result.size());
        // Slot đầu tiên (8h) phải có giá giờ vàng
        assertEquals(250.0, result.get(0).getPrice());
        // Slot thứ hai (9h) phải có giá mặc định
        assertEquals(100.0, result.get(1).getPrice());
    }

    @Test
    void generateInitialTimeSlots_Success_SkipsExistingSlots() {
        // 1. ARRANGE
        when(courtClient.getCourtById(courtId)).thenReturn(courtDto);

        // Giả lập: Slot 8h đã tồn tại
        LocalDateTime slot8am = LocalDateTime.of(2024, 1, 1, 8, 0);
        when(timeSlotRepository.existsByCourtIdAndStartTime(courtId, slot8am)).thenReturn(true);

        // Giả lập: Slot 9h chưa tồn tại
        LocalDateTime slot9am = LocalDateTime.of(2024, 1, 1, 9, 0);
        when(timeSlotRepository.existsByCourtIdAndStartTime(courtId, slot9am)).thenReturn(false);

        when(pricingRuleRepository.findApplicableRule(anyInt(), any(DayOfWeek.class), any(LocalTime.class)))
                .thenReturn(Collections.emptyList());
        when(timeSlotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. ACT
        List<TimeSlotDto> result = courtScheduleService.generateInitialTimeSlots(courtId, request, managerId);

        // 3. ASSERT
        // Kết quả trả về chỉ chứa 1 slot mới được tạo
        assertEquals(1, result.size());
        assertEquals(9, result.get(0).getStartTime().getHour());

        // Kiểm tra xem saveAll chỉ được gọi với 1 slot
        ArgumentCaptor<List<TimeSlot>> captor = ArgumentCaptor.forClass(List.class);
        verify(timeSlotRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    @Test
    void generateInitialTimeSlots_Fail_WhenUserIsNotOwner() {
        // 1. ARRANGE
        Integer otherManagerId = 99; // Một user khác không phải chủ sân
        courtDto.setOwnerId(managerId); // Sân vẫn thuộc về managerId = 1
        when(courtClient.getCourtById(courtId)).thenReturn(courtDto);

        // 2. ACT & 3. ASSERT
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            courtScheduleService.generateInitialTimeSlots(courtId, request, otherManagerId);
        });

        assertEquals("Bạn không có quyền thực hiện thao tác này trên sân của người khác.", exception.getMessage());
        verify(timeSlotRepository, never()).saveAll(anyList()); // Đảm bảo không có gì được lưu
    }

    @Test
    void generateInitialTimeSlots_Fail_WhenCourtNotFound() {
        // 1. ARRANGE
        // Giả lập: CourtClient ném ra lỗi khi không tìm thấy sân
        when(courtClient.getCourtById(courtId)).thenThrow(new ResourceNotFoundException("Sân không tồn tại"));

        // 2. ACT & 3. ASSERT
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            courtScheduleService.generateInitialTimeSlots(courtId, request, managerId);
        });

        assertTrue(exception.getMessage().contains("Sân không tồn tại"));
    }
}
