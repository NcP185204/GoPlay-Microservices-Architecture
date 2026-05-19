package com.caophuc.court.service;


import com.caophuc.court.model.CourtImage;
import org.springframework.web.multipart.MultipartFile;

public interface CourtImageService {
    CourtImage uploadCourtImage(Integer courtId, MultipartFile file, Integer managerId);
    void deleteCourtImage(Integer courtId, Integer imageId, Integer managerId);
    void setThumbnail(Integer courtId, Integer imageId, Integer managerId);
}
