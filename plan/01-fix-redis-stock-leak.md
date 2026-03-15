# 01 - Fix Redis Stock Leak

## Vấn đề

Khi Redis đã `DECR` stock thành công nhưng DB save order fail (do unique constraint violation, connection timeout, deadlock, etc.), `@Transactional` chỉ rollback DB — **Redis không được hoàn lại stock**.

Kết quả: mất slot hàng, stock bị "rò rỉ" dần.

## Code hiện tại

```java
// Redis đã trừ stock
Long remainingStock = redisTemplate.opsForValue().decrement(stockKey);

// ... check stock ...

// Nếu save fail → transaction rollback → nhưng Redis KHÔNG rollback
Order savedOrder = orderRepository.save(order);
```

## Giải pháp

Wrap phần save trong try-catch, nếu fail thì `INCREMENT` lại Redis trước khi re-throw:

```java
String stockKey = STOCK_KEY_PREFIX + flashSaleItemId;
Long remainingStock = redisTemplate.opsForValue().decrement(stockKey);

if (remainingStock == null || remainingStock < 0) {
    if (remainingStock != null) {
        redisTemplate.opsForValue().increment(stockKey);
    }
    throw new SoldOutException();
}

try {
    Order order = Order.builder()
            .userId(userId)
            .flashSaleItem(flashSaleItem)
            .productName(...)
            .price(flashSaleItem.getFlashPrice())
            .status(OrderStatus.PENDING)
            .build();

    Order savedOrder = orderRepository.save(order);
    orderRepository.flush(); // force DB write trong transaction để catch lỗi tại đây
    return flashSaleMapper.toOrderResponseDTO(savedOrder);
} catch (Exception e) {
    redisTemplate.opsForValue().increment(stockKey);
    throw e;
}
```

## File cần sửa

- `src/main/java/com/flashsale/service/FlashSaleService.java`

## Độ ưu tiên

**Cao** — Bug hiện tại, ảnh hưởng tính chính xác của stock.
