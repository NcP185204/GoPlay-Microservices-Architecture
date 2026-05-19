package com.caophuc.court.service.impl;

import com.caophuc.court.exception.ResourceNotFoundException;
import com.caophuc.court.model.Court;
import com.caophuc.court.model.CourtImage;
import com.caophuc.court.repository.CourtImageRepository;
import com.caophuc.court.repository.CourtRepository;
import com.caophuc.court.service.CourtImageService;
import com.caophuc.court.service.StorageService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CourtImageServiceImpl implements CourtImageService {

    private final CourtRepository courtRepository;
    private final CourtImageRepository courtImageRepository;
    private final StorageService storageService; // Inject Interface, không phải Class cụ thể

    @Override
    @Transactional
    public CourtImage uploadCourtImage(Integer courtId, MultipartFile file, Integer managerId) {
        Court court = findCourtById(courtId);
        checkOwnership(court, managerId);

        String imageUrl = storageService.store(file, "courts");

        CourtImage newImage = CourtImage.builder()
                .imageUrl(imageUrl)
                .court(court)
                .build();

        if (court.getThumbnailUrl() == null || court.getThumbnailUrl().isEmpty()) {
            court.setThumbnailUrl(imageUrl);
            courtRepository.save(court);
        }

        return courtImageRepository.save(newImage);
    }

    @Override
    @Transactional
    public void deleteCourtImage(Integer courtId, Integer imageId, Integer managerId) {
        Court court = findCourtById(courtId);
        checkOwnership(court, managerId);

        CourtImage image = courtImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh"));

        if (!image.getCourt().getId().equals(courtId)) {
            throw new RuntimeException("Ảnh không thuộc về sân này");
        }

        storageService.delete(image.getImageUrl());
        courtImageRepository.delete(image);

        if (image.getImageUrl().equals(court.getThumbnailUrl())) {
            court.setThumbnailUrl(null);
            courtRepository.save(court);
        }
    }

    @Override
    @Transactional
    public void setThumbnail(Integer courtId, Integer imageId, Integer managerId) {
        Court court = findCourtById(courtId);
        checkOwnership(court, managerId);

        CourtImage image = courtImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh"));

        if (!image.getCourt().getId().equals(courtId)) {
            throw new RuntimeException("Ảnh không thuộc về sân này");
        }

        court.setThumbnailUrl(image.getImageUrl());
        courtRepository.save(court);
    }

    // Helper methods (DRY: Có thể tách ra class Utility riêng nếu muốn dùng chung)
    private Court findCourtById(Integer courtId) {
        return courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Sân không tồn tại"));
    }

    private void checkOwnership(Court court, Integer currentUserId) {
        // Since we are in microservice architecture, roles are handled by API Gateway,
        // so we check if the current user is the owner of the court.
        boolean isOwner = Objects.equals(court.getOwnerId(), currentUserId);
        
        if (!isOwner) {
            throw new RuntimeException("Bạn không có quyền thực hiện hành động này.");
        }
    }
}
