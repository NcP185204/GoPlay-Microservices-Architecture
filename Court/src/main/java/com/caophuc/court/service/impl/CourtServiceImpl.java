package com.caophuc.court.service.impl;


import com.caophuc.court.dto.CourtDetailResponse;
import com.caophuc.court.dto.CourtSearchCriteria;
import com.caophuc.court.dto.CourtSummaryResponse;
import com.caophuc.court.dto.CreateCourtRequest;
import com.caophuc.court.exception.ResourceNotFoundException;
import com.caophuc.court.model.Court;
import com.caophuc.court.model.CourtImage;
import com.caophuc.court.repository.CourtRepository;
import com.caophuc.court.service.CourtService;
import com.caophuc.court.service.specification.CourtSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourtServiceImpl implements CourtService {

    private final CourtRepository courtRepository;
    private final CourtSpecification courtSpecification;

    @Override
    @Transactional
    public CourtDetailResponse createCourt(CreateCourtRequest request, Integer ownerId) {
        Court court = Court.builder()
                .name(request.getName())
                .address(request.getAddress())
                .description(request.getDescription())
                .courtType(request.getCourtType())
                .pricePerHour(request.getPricePerHour())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .averageRating(0.0)
                .ownerId(ownerId)
                .services(request.getServices())
                .build();
        return mapToResponse(courtRepository.save(court));
    }

    @Override
    @Transactional
    public CourtDetailResponse updateCourt(Integer courtId, CreateCourtRequest request, Integer currentUserId) {
        Court court = findCourtById(courtId);
        checkOwnership(court, currentUserId);

        court.setName(request.getName());
        court.setAddress(request.getAddress());
        court.setDescription(request.getDescription());
        court.setCourtType(request.getCourtType());
        court.setPricePerHour(request.getPricePerHour());
        court.setLatitude(request.getLatitude());
        court.setLongitude(request.getLongitude());
        court.setServices(request.getServices());

        return mapToResponse(courtRepository.save(court));
    }

    @Override
    @Transactional
    public void deleteCourt(Integer courtId, Integer currentUserId) {
        Court court = findCourtById(courtId);
        checkOwnership(court, currentUserId);
        courtRepository.delete(court);
    }

    @Override
    public CourtDetailResponse getCourtById(Integer courtId) {
        return mapToResponse(findCourtById(courtId));
    }

    @Override
    public Page<CourtSummaryResponse> searchCourts(CourtSearchCriteria criteria, Pageable pageable) {
        Page<Court> courts = courtRepository.findAll(courtSpecification.build(criteria), pageable);
        return courts.map(this::mapToSummaryResponse);
    }

    private Court findCourtById(Integer courtId) {
        return courtRepository.findById(courtId).orElseThrow(() -> new ResourceNotFoundException("Sân không tồn tại"));
    }

    private void checkOwnership(Court court, Integer currentUserId) {
        // Since we are in microservice architecture, roles are handled by API Gateway,
        // so we check if the current user is the owner of the court.
        boolean isOwner = Objects.equals(court.getOwnerId(), currentUserId);
        
        if (!isOwner) {
            throw new RuntimeException("Bạn không có quyền thực hiện hành động này.");
        }
    }

    private CourtDetailResponse mapToResponse(Court c) {
        List<String> imageUrls = c.getImages().stream().map(CourtImage::getImageUrl).collect(Collectors.toList());
        return CourtDetailResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .address(c.getAddress())
                .description(c.getDescription())
                .courtType(c.getCourtType())
                .pricePerHour(c.getPricePerHour())
                .averageRating(c.getAverageRating())
                .ownerId(c.getOwnerId())
                .thumbnailUrl(c.getThumbnailUrl())
                .imageUrls(imageUrls)
                .services(c.getServices())
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .build();
    }
    
    private CourtSummaryResponse mapToSummaryResponse(Court c) {
        return CourtSummaryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .address(c.getAddress())
                .courtType(c.getCourtType())
                .pricePerHour(c.getPricePerHour())
                .averageRating(c.getAverageRating())
                .thumbnailUrl(c.getThumbnailUrl())
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .build();
    }
}
