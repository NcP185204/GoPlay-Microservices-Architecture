package com.caophuc.booking.service.impl;

import com.caophuc.booking.client.CourtClient;
import com.caophuc.booking.client.CourtDto;
import com.caophuc.booking.dto.GenerateTimeSlotRequest;
import com.caophuc.booking.dto.PricingRuleDto;
import com.caophuc.booking.dto.TimeSlotDto;
import com.caophuc.booking.exception.ResourceNotFoundException;
import com.caophuc.booking.exception.AccessDeniedException;
import com.caophuc.booking.model.PricingRule;
import com.caophuc.booking.model.TimeSlot;
import com.caophuc.booking.repository.PricingRuleRepository;
import com.caophuc.booking.repository.TimeSlotRepository;
import com.caophuc.booking.service.CourtScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourtScheduleServiceImpl implements CourtScheduleService {

    private final TimeSlotRepository timeSlotRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final CourtClient courtClient;

    /**
     * Lấy danh sách các khung giờ còn trống (có thể đặt) của một sân cụ thể trong một ngày.
     * Hàm này sẽ:
     * 1. Kiểm tra xem sân có tồn tại không (gọi qua Court Service).
     * 2. Tìm tất cả TimeSlot của sân đó từ 00:00:00 đến 23:59:59 của ngày được yêu cầu.
     * 3. Chuyển đổi (Map) các entity TimeSlot sang đối tượng TimeSlotDto để trả về cho người dùng.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotDto> getAvailableTimeSlots(Integer courtId, LocalDate date) {
        checkCourtExists(courtId);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        List<TimeSlot> slots = timeSlotRepository.findSlotsByCourtAndDate(courtId, startOfDay, endOfDay);
        return slots.stream().map(this::mapToTimeSlotDto).collect(Collectors.toList());
    }

    /**
     * Tạo hàng loạt khung giờ (TimeSlot) ban đầu cho một sân. Dành cho chủ sân thiết lập lịch hoạt động.
     * Hàm này sẽ:
     * 1. Kiểm tra sân có tồn tại và người gọi (managerId) có phải là chủ sân không.
     * 2. Vòng lặp từng ngày (từ ngày bắt đầu đến số ngày được yêu cầu).
     * 3. Trong mỗi ngày, vòng lặp tạo các khung giờ (ví dụ: mỗi 60 phút) từ giờ mở đến giờ đóng cửa.
     * 4. Kiểm tra khung giờ đó đã được tạo trước đó chưa, nếu chưa thì tính giá tiền (dựa vào PricingRule hoặc giá mặc định).
     * 5. Lưu tất cả khung giờ mới tạo vào database.
     */
    @Override
    @Transactional
    public List<TimeSlotDto> generateInitialTimeSlots(Integer courtId, GenerateTimeSlotRequest request, Integer managerId) {
        // Kiểm tra quyền: Người tạo lịch phải là chủ sân
        CourtDto courtDto = checkCourtExistsAndOwnership(courtId, managerId);

        List<TimeSlot> newSlots = new ArrayList<>();

        LocalDate startDate = request.getStartDate();
        int numberOfDays = request.getNumberOfDays();
        int slotDurationInMinutes = request.getSlotDurationInMinutes();

        // Lặp qua từng ngày
        for (int i = 0; i < numberOfDays; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            LocalDateTime slotTime = currentDate.atTime(request.getOpenTime());
            LocalDateTime closeTime = currentDate.atTime(request.getCloseTime());

            // Lặp qua các khung giờ trong 1 ngày
            while (slotTime.isBefore(closeTime)) {
                LocalDateTime startTime = slotTime;
                LocalDateTime endTime = startTime.plusMinutes(slotDurationInMinutes);
                
                // Nếu thời gian kết thúc vượt quá giờ đóng cửa thì dừng lại
                if (endTime.isAfter(closeTime)) break;

                // Chỉ tạo nếu khung giờ đó chưa tồn tại trong database (tránh trùng lặp)
                if (!timeSlotRepository.existsByCourtIdAndStartTime(courtId, startTime)) {
                    // Tính giá tiền cho khung giờ này
                    Double price = findPriceForSlot(courtId, startTime, courtDto);
                    
                    TimeSlot slot = TimeSlot.builder()
                            .courtId(courtId)
                            .startTime(startTime)
                            .endTime(endTime)
                            .isAvailable(true)
                            .price(price)
                            .build();
                    newSlots.add(slot);
                }
                // Tiến tới khung giờ tiếp theo
                slotTime = endTime;
            }
        }
        
        // Lưu tất cả các khung giờ hợp lệ xuống database
        List<TimeSlot> savedSlots = timeSlotRepository.saveAll(newSlots);
        return savedSlots.stream().map(this::mapToTimeSlotDto).collect(Collectors.toList());
    }

    /**
     * Tạo một quy tắc giá mới (PricingRule) cho một sân. Dành cho chủ sân.
     * Ví dụ: Thiết lập giá đặc biệt 200k cho khung giờ 18h-20h ngày Chủ Nhật.
     */
    @Override
    @Transactional
    public PricingRule setPricingRule(Integer courtId, PricingRuleDto dto, Integer managerId) {
        // Kiểm tra quyền: Chỉ chủ sân mới được thiết lập giá
        checkCourtExistsAndOwnership(courtId, managerId);
        
        PricingRule rule = PricingRule.builder()
                .courtId(courtId)
                .dayOfWeek(dto.getDayOfWeek())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .price(dto.getPrice())
                .build();
        return pricingRuleRepository.save(rule);
    }

    /**
     * Lấy danh sách tất cả các quy tắc giá đã được thiết lập của một sân.
     */
    @Override
    public List<PricingRule> getPricingRules(Integer courtId) {
        return pricingRuleRepository.findByCourtId(courtId);
    }

    /**
     * Xóa một quy tắc giá khỏi hệ thống.
     */
    @Override
    @Transactional
    public void deletePricingRule(Integer courtId, Integer ruleId, Integer managerId) {
        // Kiểm tra quyền: Chỉ chủ sân mới được xóa quy tắc giá của sân đó
        checkCourtExistsAndOwnership(courtId, managerId);
        pricingRuleRepository.deleteById(ruleId);
    }

    /**
     * (Hàm bổ trợ) Tính toán giá tiền thực tế cho một khung giờ cụ thể (TimeSlot).
     * Hàm này ưu tiên:
     * 1. Tìm xem có PricingRule nào áp dụng cho (thứ trong tuần + giờ) này không. Nếu có thì lấy giá đó.
     * 2. Nếu không có PricingRule nào, thì lấy giá mặc định (pricePerHour) của sân.
     * 3. Nếu sân chưa cấu hình giá mặc định, trả về giá dự phòng là 100.0.
     */
    private Double findPriceForSlot(Integer courtId, LocalDateTime startTime, CourtDto courtDto) {
        DayOfWeek dayOfWeek = startTime.getDayOfWeek();
        LocalTime time = startTime.toLocalTime();
        
        // Tìm quy tắc giá khớp với ngày và giờ
        List<PricingRule> applicableRules = pricingRuleRepository.findApplicableRule(courtId, dayOfWeek, time);
        
        if (!applicableRules.isEmpty()) {
            return applicableRules.get(0).getPrice();
        }
        
        // Fallback về giá cơ bản của sân, nếu không có thì mặc định 100.0
        return courtDto.getPricePerHour() != null ? courtDto.getPricePerHour() : 100.0;
    }

    /**
     * (Hàm bổ trợ) Chuyển đổi đối tượng Entity TimeSlot thành DTO để trả về qua API.
     */
    private TimeSlotDto mapToTimeSlotDto(TimeSlot slot) {
        return TimeSlotDto.builder()
                .id(slot.getId())
                .courtId(slot.getCourtId())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .isAvailable(slot.isAvailable())
                .price(slot.getPrice())
                .build();
    }

    /**
     * (Hàm bổ trợ) Gọi sang Court Service để kiểm tra sân có tồn tại hay không.
     */
    private CourtDto checkCourtExists(Integer courtId) {
        try {
            CourtDto court = courtClient.getCourtById(courtId);
            if (court == null) {
                throw new ResourceNotFoundException("Sân không tồn tại");
            }
            return court;
        } catch (Exception e) {
            throw new ResourceNotFoundException("Sân không tồn tại hoặc Court Service không phản hồi");
        }
    }

    /**
     * (Hàm bổ trợ) Gọi sang Court Service để kiểm tra sân, đồng thời xác minh người gọi có phải chủ sân hay không.
     * Nếu không phải chủ sân, sẽ ném ra lỗi AccessDeniedException (từ chối truy cập).
     */
    private CourtDto checkCourtExistsAndOwnership(Integer courtId, Integer managerId) {
        CourtDto court = checkCourtExists(courtId);
        // So sánh ID của người sở hữu sân (do Court Service trả về) với ID của người dùng đang gọi API
        if (!Objects.equals(court.getOwnerId(), managerId)) {
             throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này trên sân của người khác.");
        }
        return court;
    }
}