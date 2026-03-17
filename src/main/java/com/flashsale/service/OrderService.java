package com.flashsale.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flashsale.entity.FlashSaleItem;
import com.flashsale.entity.Order;
import com.flashsale.enums.OrderStatus;
import com.flashsale.messaging.OrderMessage;
import com.flashsale.repository.FlashSaleItemRepository;
import com.flashsale.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    
    @Transactional
    public void createOrder(OrderMessage orderMessage) {
        FlashSaleItem flashSaleItem = flashSaleItemRepository
                .getReferenceById(orderMessage.getFlashSaleItemId());
    
        Order order = Order.builder()
            .userId(orderMessage.getUserId())
            .flashSaleItem(flashSaleItem)
            .productName(orderMessage.getProductName())
            .price(orderMessage.getPrice())
            .status(OrderStatus.PENDING)
            .build();

        orderRepository.save(order);
        log.info("Order created: userId={}", orderMessage.getUserId());
    }
}
