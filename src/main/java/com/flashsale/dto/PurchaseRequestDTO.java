package com.flashsale.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestDTO {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "flashSaleItemId is required")
    private Long flashSaleItemId;
}
