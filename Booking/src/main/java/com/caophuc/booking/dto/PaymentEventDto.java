package com.caophuc.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventDto {
    private Integer bookingId;
    private String status; // Ví dụ: "SUCCESS", "FAILED"
    private String message; 
}