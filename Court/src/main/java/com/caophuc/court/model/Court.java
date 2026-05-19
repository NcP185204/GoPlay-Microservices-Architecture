package com.caophuc.court.model;


import com.caophuc.court.util.FacilityService;
import com.caophuc.court.util.SportType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "courts", indexes = {
        @Index(name = "idx_court_name", columnList = "name"),
        @Index(name = "idx_court_type", columnList = "courtType"), // Đảm bảo bạn có trường courtType
        @Index(name = "idx_price", columnList = "pricePerHour")
})
public class Court {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // --- SỬA Ở ĐÂY: Chỉ lưu ID của chủ sân ---
    @Column(name = "owner_id", nullable = false)
    private Integer ownerId;

    private String name;
    private String address;
    private String description;

    // --- Đã bỏ @Enumerated vì giá tiền không phải là Enum ---
    private Double pricePerHour;

    // Bạn đang thiếu trường courtType (dựa vào cái @Index ở trên thì cần có)
    @Enumerated(EnumType.STRING)
    private SportType courtType;

    private Double latitude;
    private Double longitude;

    @Builder.Default
    private Double averageRating = 0.0;

    private String thumbnailUrl;

    @OneToMany(mappedBy = "court", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CourtImage> images = new ArrayList<>();

    @ElementCollection(targetClass = FacilityService.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "court_services", joinColumns = @JoinColumn(name = "court_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "service_name", nullable = false)
    private Set<FacilityService> services;
}