package com.flashsale.messaging.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.flashsale.messaging.OrderMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderProducer {
    private final RabbitTemplate rabbitTemplate;

    public void publishOrder(OrderMessage orderMessage) {
        log.info("Publishing order message");
        rabbitTemplate.convertAndSend("order.exchange", "order.key", orderMessage);
    }
}
