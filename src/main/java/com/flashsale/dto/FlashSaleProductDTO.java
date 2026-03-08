package com.flashsale.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleProductDTO {

    private Long productId;
    private String brand;
    private String model;
    private String storage;
    private String color;
    private Long flashPrice;
    private Integer remainingStock;
}
