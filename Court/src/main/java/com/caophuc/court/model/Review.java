package com.caophuc.court.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Class Review: Đại diện cho thực thể Đánh giá (Review), ánh xạ tới bảng "reviews" trong Database.
 */
@Data // Tự động sinh Getter, Setter, toString, equals, hashCode
@Builder // Hỗ trợ khởi tạo đối tượng bằng Builder Pattern
@NoArgsConstructor // Sinh Constructor không tham số (cần thiết cho JPA)
@AllArgsConstructor // Sinh Constructor với tất cả tham số
@Entity // Đánh dấu đây là một Entity JPA
@Table(name = "reviews") // Tên bảng trong cơ sở dữ liệu sẽ là "reviews"
public class Review {

    // --- KHÓA CHÍNH (PRIMARY KEY) ---
    @Id // Đánh dấu là khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Cho phép DB tự động tăng giá trị (Auto Increment)
    private Integer id;

    // --- MỐI QUAN HỆ VỚI SÂN (COURT) ---
    @ManyToOne(fetch = FetchType.LAZY)
    // Quan hệ N-1: Nhiều đánh giá thuộc về Một sân.
    // LAZY: Chỉ tải thông tin Court khi gọi phương thức getCourt(), tối ưu hiệu năng.
    @JoinColumn(name = "court_id")
    // Định nghĩa cột khóa ngoại là 'court_id', trỏ đến sân được đánh giá.
    private Court court;

    // --- NGƯỜI CHƠI (PLAYER ID) ---
    @Column(name = "player_id")
    private Integer playerId;

    // --- NỘI DUNG ĐÁNH GIÁ ---
    private int rating; // Điểm đánh giá (thường là 1-5 sao)
    private String comment; // Nội dung bình luận/đánh giá

    // --- THỜI GIAN TẠO ---
    @Builder.Default
    // Nếu dùng Builder mà không set giá trị này, nó sẽ dùng giá trị mặc định.
    private LocalDateTime createdAt = LocalDateTime.now();
    // Lưu thời điểm tạo đánh giá, tự động lấy thời gian hiện tại khi khởi tạo.
}
