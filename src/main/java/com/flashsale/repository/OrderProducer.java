package com.flashsale.repository;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProducer {
    
    private final RabbitTemplate rabbitTemplate;

    public void publishOrder(String orderKey) {
        rabbitTemplate.convertAndSend("order.exchange", "order.key", order);
    }
    
}
