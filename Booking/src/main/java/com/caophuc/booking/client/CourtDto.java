package com.caophuc.booking.client;

import lombok.Data;

@Data
public class CourtDto {
    private Integer id;
    private String name;
    private String address;
    private Integer ownerId;
    private Double pricePerHour;
}