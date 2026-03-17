package com.flashsale.messaging;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage {
    private Long userId;
    private Long flashSaleItemId;
    private String productName;
    private Long price;
    private String orderKey;
    private LocalDateTime createdAt;
}