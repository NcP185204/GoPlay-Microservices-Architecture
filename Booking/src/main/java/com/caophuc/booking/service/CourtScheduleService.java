package com.caophuc.booking.service;


import com.caophuc.booking.dto.GenerateTimeSlotRequest;
import com.caophuc.booking.dto.PricingRuleDto;
import com.caophuc.booking.dto.TimeSlotDto;
import com.caophuc.booking.model.PricingRule;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface định nghĩa các nghiệp vụ liên quan đến lịch sân và giá cả.
 * Bao gồm việc tạo/xem khung giờ (TimeSlot) và quản lý các quy tắc giá (PricingRule).
 */
public interface CourtScheduleService {

    // --- TimeSlot methods ---

    /**
     * Lấy danh sách các khung giờ còn trống của một sân cụ thể vào một ngày cụ thể.
     * Dành cho người dùng (user) xem lịch.
     * @param courtId ID của sân cần xem.
     * @param date Ngày cần xem.
     * @return Danh sách các TimeSlotDto còn trống.
     */
    List<TimeSlotDto> getAvailableTimeSlots(Integer courtId, LocalDate date);

    /**
     * (Dành cho chủ sân) Tạo hàng loạt các khung giờ ban đầu cho sân của mình.
     * Ví dụ: Tạo lịch cho 7 ngày tới, mỗi ngày từ 8h-22h, mỗi slot 60 phút.
     * @param courtId ID của sân cần tạo lịch.
     * @param request DTO chứa thông tin (ngày bắt đầu, số ngày, giờ mở/đóng cửa, ...).
     * @param managerId ID của người quản lý (chủ sân) đang thực hiện. Dùng để xác thực quyền.
     * @return Danh sách các TimeSlotDto vừa được tạo.
     */
    List<TimeSlotDto> generateInitialTimeSlots(Integer courtId, GenerateTimeSlotRequest request, Integer managerId);


    // --- PricingRule methods ---

    /**
     * (Dành cho chủ sân) Thiết lập một quy tắc giá mới cho sân.
     * Ví dụ: Set giá 200k cho khung giờ 18h-20h vào các ngày cuối tuần.
     * @param courtId ID của sân.
     * @param dto DTO chứa thông tin về quy tắc giá (ngày trong tuần, giờ bắt đầu/kết thúc, giá tiền).
     * @param managerId ID của chủ sân để xác thực.
     * @return Đối tượng PricingRule vừa được tạo.
     */
    PricingRule setPricingRule(Integer courtId, PricingRuleDto dto, Integer managerId);

    /**
     * Lấy tất cả các quy tắc giá đã được thiết lập cho một sân.
     * @param courtId ID của sân.
     * @return Danh sách các PricingRule.
     */
    List<PricingRule> getPricingRules(Integer courtId);

    /**
     * (Dành cho chủ sân) Xóa một quy tắc giá.
     * @param courtId ID của sân.
     * @param ruleId ID của quy tắc giá cần xóa.
     * @param managerId ID của chủ sân để xác thực.
     */
    void deletePricingRule(Integer courtId, Integer ruleId, Integer managerId);
}
