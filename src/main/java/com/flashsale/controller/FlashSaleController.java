package com.flashsale.controller;

import com.flashsale.dto.FlashSaleProductDTO;
import com.flashsale.dto.OrderResponseDTO;
import com.flashsale.dto.PurchaseRequestDTO;
import com.flashsale.service.FlashSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flash-sale")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    @GetMapping("/products")
    public ResponseEntity<List<FlashSaleProductDTO>> getFlashSaleProducts() {
        return ResponseEntity.ok(flashSaleService.getFlashSaleProducts());
    }

    @PostMapping("/purchase")
    public ResponseEntity<OrderResponseDTO> purchaseFlashSaleItem(
            @Valid @RequestBody PurchaseRequestDTO request) {
        return ResponseEntity.ok(flashSaleService.purchaseFlashSaleItem(request));
    }

    @PostMapping("/warm-up")
    public ResponseEntity<Map<String, Object>> warmUpStock() {
        int count = flashSaleService.warmUpStock();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "itemsLoaded", count));
    }
}
