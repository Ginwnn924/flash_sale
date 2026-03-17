package com.flashsale.messaging.consumer;

import java.io.IOException;
import java.time.Duration;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.flashsale.constant.RedisKeys;
import com.flashsale.messaging.OrderMessage;
import com.flashsale.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderConsumer {

    private final StringRedisTemplate redisTemplate;
    private final OrderService orderService;

    @RabbitListener(queues = "order.queue")
    public void consumeOrder(OrderMessage orderMessage,
                             Channel channel,
                             @Header("amqp_deliveryTag") long tag) throws IOException {
        try {
    
            orderService.createOrder(orderMessage);

            // Đổi state từ QUEUED sang CONFIRMED
            redisTemplate.opsForValue().set(
                orderMessage.getOrderKey(), "CONFIRMED",
                Duration.ofHours(24)
            );
            // Thành công → ACK
            channel.basicAck(tag, false);
    
        } catch (Exception e) {
            log.error("Error processing order: {}", e.getMessage());
            // Thất bại → NACK, không requeue → đẩy vào DLQ
            channel.basicNack(tag, false, false);
        }
    }
    
    @RabbitListener(queues = "order.dlq")
    public void handleDeadLetter(OrderMessage orderMessage,
                                  Channel channel,
                                  @Header("amqp_deliveryTag") long tag) throws IOException {
        log.error("DEAD LETTER: userId={}, itemId={}", 
            orderMessage.getUserId(), orderMessage.getFlashSaleItemId());
    
        // Rollback stock
        redisTemplate.opsForValue()
            .increment(RedisKeys.STOCK_KEY_PREFIX + orderMessage.getFlashSaleItemId());
    
        // Xóa orderKey để user có thể retry
        redisTemplate.delete(orderMessage.getOrderKey());
    
        channel.basicAck(tag, false);
    }
}
