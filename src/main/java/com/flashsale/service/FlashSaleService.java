package com.flashsale.service;

import com.flashsale.constant.RedisKeys;
import com.flashsale.dto.FlashSaleProductDTO;
import com.flashsale.dto.OrderResponseDTO;
import com.flashsale.dto.PurchaseRequestDTO;
import com.flashsale.entity.FlashSaleItem;
import com.flashsale.exception.DuplicateOrderException;
import com.flashsale.exception.FlashSaleNotActiveException;
import com.flashsale.exception.SoldOutException;
import com.flashsale.mapper.FlashSaleMapper;
import com.flashsale.messaging.OrderMessage;
import com.flashsale.messaging.producer.OrderProducer;
import com.flashsale.repository.FlashSaleItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlashSaleService {

    private final FlashSaleItemRepository flashSaleItemRepository;
    private final StringRedisTemplate redisTemplate;
    private final FlashSaleMapper flashSaleMapper;
    private final OrderProducer orderProducer;

    public List<FlashSaleProductDTO> getFlashSaleProducts() {
        List<FlashSaleItem> activeItems = flashSaleItemRepository
                .findActiveFlashSaleItems(LocalDateTime.now());

        return activeItems.stream()
                .map(item -> {
                    FlashSaleProductDTO dto = flashSaleMapper.toFlashSaleProductDTO(item);
                    String stockKey = RedisKeys.STOCK_KEY_PREFIX + item.getId();
                    String cachedStock = redisTemplate.opsForValue().get(stockKey);
                    if (cachedStock != null) {
                        dto.setRemainingStock(Integer.parseInt(cachedStock));
                    }
                    return dto;
                })
                .toList();
    }

    public OrderResponseDTO purchaseFlashSaleItem(PurchaseRequestDTO request) {
        Long userId = request.getUserId();
        Long flashSaleItemId = request.getFlashSaleItemId();

        FlashSaleItem flashSaleItem = flashSaleItemRepository.findById(flashSaleItemId)
                .orElseThrow(() -> new EntityNotFoundException("Flash sale item not found: " + flashSaleItemId));

        if (!flashSaleItem.isActive()) {
            throw new FlashSaleNotActiveException();
        }


        String stockKey = RedisKeys.STOCK_KEY_PREFIX + flashSaleItemId;
        Long remainingStock = redisTemplate.opsForValue().decrement(stockKey);

        if (remainingStock == null || remainingStock < 0) {
            if (remainingStock != null) {
                redisTemplate.opsForValue().increment(stockKey);
            }
            throw new SoldOutException();
        }

        String orderKey = RedisKeys.ORDER_KEY_PREFIX + flashSaleItemId + ":" + userId;
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(orderKey, "QUEUED", Duration.ofHours(24));
        
        if (Boolean.FALSE.equals(isNew)) {
            redisTemplate.opsForValue().increment(stockKey);
            throw new DuplicateOrderException();
        }

        // send message to rabbitmq
        OrderMessage orderMessage = OrderMessage.builder()
                .userId(userId)
                .flashSaleItemId(flashSaleItemId)
                .productName(flashSaleItem.getProduct().getBrand() + " " + flashSaleItem.getProduct().getModel())
                .price(flashSaleItem.getFlashPrice())
                .orderKey(orderKey)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            orderProducer.publishOrder(orderMessage);
        } catch (Exception e) {
            // Rollback stock
            redisTemplate.opsForValue().increment(stockKey);
            redisTemplate.delete(orderKey);
            throw new RuntimeException("Không thể xử lý đơn hàng, vui lòng thử lại");
        }
        return OrderResponseDTO.builder()
                .status("QUEUED")
                .message("Đơn hàng đang xử lí. Xin vui lòng chờ.")
                .build();


    }

    public int warmUpStock() {
        List<FlashSaleItem> items = flashSaleItemRepository.findAll();
        items.forEach(item -> {
            String key = RedisKeys.STOCK_KEY_PREFIX + item.getId();
            redisTemplate.opsForValue().set(key, String.valueOf(item.getTotalStock()));
            log.info("Loaded stock: {} = {}", key, item.getTotalStock());
        });
        log.info("Redis stock warm-up complete: {} items", items.size());
        return items.size();
    }
}
