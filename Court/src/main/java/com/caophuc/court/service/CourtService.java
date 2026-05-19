package com.caophuc.court.service;

import com.caophuc.court.dto.CourtDetailResponse;
import com.caophuc.court.dto.CourtSearchCriteria;
import com.caophuc.court.dto.CourtSummaryResponse;
import com.caophuc.court.dto.CreateCourtRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourtService {

    // --- Operations CRUD ---
    CourtDetailResponse createCourt(CreateCourtRequest request, Integer ownerId);
    CourtDetailResponse updateCourt(Integer courtId, CreateCourtRequest request, Integer currentUserId);
    void deleteCourt(Integer courtId, Integer currentUserId);

    // --- Operations Đọc & Tìm kiếm ---
    CourtDetailResponse getCourtById(Integer courtId);
    Page<CourtSummaryResponse> searchCourts(CourtSearchCriteria criteria, Pageable pageable);
}
