package com.caophuc.court.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "favorite_courts")
public class FavoriteCourt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // CHỈ LƯU ID CỦA USER, KHÔNG dùng @ManyToOne vì User ở DB khác
        @Column(name = "user_id", nullable = false)
        private Integer userId;

    //    // Quan hệ với bảng Court (Được phép vì nó nằm cùng 1 Service)
    //    @ManyToOne(fetch = FetchType.LAZY)
    //    @JoinColumn(name = "court_id", nullable = false)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

