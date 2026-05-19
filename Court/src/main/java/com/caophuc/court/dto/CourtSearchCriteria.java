package com.caophuc.court.dto;

import com.caophuc.court.util.SportType;
import lombok.Data;

@Data
public class CourtSearchCriteria {
    private String name;
    private SportType courtType;
    private Double minPrice;
    private Double maxPrice;
    private Double minRating;
    private Double latitude; // Vị trí người dùng
    private Double longitude;
    private Double radiusInKm;
}
