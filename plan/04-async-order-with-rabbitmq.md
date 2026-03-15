# 04 - Async Order Processing with RabbitMQ

## Mục tiêu

Tách DB write ra khỏi request path. Sau khi Redis trừ stock, publish message cho RabbitMQ, consumer save order vào DB background.

## Flow mới

```
Request path (nhanh):
  1. SETNX Redis → chặn duplicate (plan 02)
  2. DECR Redis → trừ stock
  3. Publish message → RabbitMQ (with publisher confirm)
  4. Response: { status: "QUEUED" }

  Nếu step 2 fail: DELETE purchased key → throw SoldOutException
  Nếu step 3 fail: INCR stock + DELETE purchased key → throw error

Consumer (background):
  1. Consume message
  2. Save order to Postgres
  3. Manual ACK
  4. Nếu save fail → NACK → retry → sau N lần → Dead Letter Queue
```

## Cần làm

### Infrastructure
- [ ] Thêm RabbitMQ vào `docker-compose.yml`
- [ ] Thêm `spring-boot-starter-amqp` vào `pom.xml`
- [ ] Config RabbitMQ trong `application.yml`

### Code - Producer
- [ ] Tạo `OrderMessage` DTO (userId, flashSaleItemId, productName, price)
- [ ] Tạo `RabbitMQConfig` class (exchange, queue, binding, DLQ)
- [ ] Sửa `FlashSaleService.purchaseFlashSaleItem()` → publish thay vì save

### Code - Consumer
- [ ] Tạo `OrderConsumer` class với `@RabbitListener`
- [ ] Manual ACK mode
- [ ] Save order to DB
- [ ] Handle failure → NACK → retry logic

### API Response
- [ ] Sửa `OrderResponseDTO` → trả `status: "QUEUED"` thay vì `orderId`
- [ ] (Optional) Thêm endpoint GET `/orders/status/{requestId}` để client poll

### K6 Test
- [ ] Sửa k6 test check cho response mới (không còn `orderId` ngay)

## Dependencies (phải làm trước)

- Plan 01 (fix stock leak) — pattern try-catch rollback Redis vẫn cần
- Plan 02 (duplicate check Redis) — bắt buộc vì DB chưa có order tại thời điểm check

## Độ ưu tiên

**Thấp** — Enhancement lớn, làm sau khi 01 + 02 ổn định.
