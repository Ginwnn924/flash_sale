package com.flashsale.service;

import com.flashsale.dto.FlashSaleProductDTO;
import com.flashsale.dto.OrderResponseDTO;
import com.flashsale.dto.PurchaseRequestDTO;
import com.flashsale.entity.FlashSaleItem;
import com.flashsale.entity.Order;
import com.flashsale.enums.OrderStatus;
import com.flashsale.exception.DuplicateOrderException;
import com.flashsale.exception.FlashSaleNotActiveException;
import com.flashsale.exception.SoldOutException;
import com.flashsale.mapper.FlashSaleMapper;
import com.flashsale.repository.FlashSaleItemRepository;
import com.flashsale.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlashSaleService {

    private static final String STOCK_KEY_PREFIX = "stock:flashsale:";

    private final FlashSaleItemRepository flashSaleItemRepository;
    private final OrderRepository orderRepository;
    private final StringRedisTemplate redisTemplate;
    private final FlashSaleMapper flashSaleMapper;

    public List<FlashSaleProductDTO> getFlashSaleProducts() {
        List<FlashSaleItem> activeItems = flashSaleItemRepository
                .findActiveFlashSaleItems(LocalDateTime.now());

        return activeItems.stream()
                .map(item -> {
                    FlashSaleProductDTO dto = flashSaleMapper.toFlashSaleProductDTO(item);
                    String stockKey = STOCK_KEY_PREFIX + item.getId();
                    String cachedStock = redisTemplate.opsForValue().get(stockKey);
                    if (cachedStock != null) {
                        dto.setRemainingStock(Integer.parseInt(cachedStock));
                    }
                    return dto;
                })
                .toList();
    }

    @Transactional
    public OrderResponseDTO purchaseFlashSaleItem(PurchaseRequestDTO request) {
        Long userId = request.getUserId();
        Long flashSaleItemId = request.getFlashSaleItemId();

        FlashSaleItem flashSaleItem = flashSaleItemRepository.findById(flashSaleItemId)
                .orElseThrow(() -> new EntityNotFoundException("Flash sale item not found: " + flashSaleItemId));

        if (!flashSaleItem.isActive()) {
            throw new FlashSaleNotActiveException();
        }

        if (orderRepository.existsByUserIdAndFlashSaleItemId(userId, flashSaleItemId)) {
            throw new DuplicateOrderException();
        }

        String stockKey = STOCK_KEY_PREFIX + flashSaleItemId;
        Long remainingStock = redisTemplate.opsForValue().decrement(stockKey);

        if (remainingStock == null || remainingStock < 0) {
            if (remainingStock != null) {
                redisTemplate.opsForValue().increment(stockKey);
            }
            throw new SoldOutException();
        }

        Order order = Order.builder()
                .userId(userId)
                .flashSaleItem(flashSaleItem)
                .product(flashSaleItem.getProduct())
                .productName(flashSaleItem.getProduct().getBrand() + " " + flashSaleItem.getProduct().getModel())
                .price(flashSaleItem.getFlashPrice())
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created: userId={}, orderId={}, flashSaleItemId={}", userId, savedOrder.getId(),
                flashSaleItemId);

        return flashSaleMapper.toOrderResponseDTO(savedOrder);
    }

    public int warmUpStock() {
        List<FlashSaleItem> items = flashSaleItemRepository.findAll();
        items.forEach(item -> {
            String key = STOCK_KEY_PREFIX + item.getId();
            redisTemplate.opsForValue().set(key, String.valueOf(item.getTotalStock()));
            log.info("Loaded stock: {} = {}", key, item.getTotalStock());
        });
        log.info("Redis stock warm-up complete: {} items", items.size());
        return items.size();
    }
}
