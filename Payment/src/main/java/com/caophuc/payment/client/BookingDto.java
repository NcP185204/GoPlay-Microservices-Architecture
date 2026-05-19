package com.caophuc.payment.client;

import lombok.Data;

@Data
public class BookingDto {
    private Integer id;
    private Integer userId;
    private Double totalPrice;
    private String status;
    private String paymentMethod;
}