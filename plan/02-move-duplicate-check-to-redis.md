# 02 - Move Duplicate Check to Redis

## Vấn đề

Hiện tại duplicate check dùng DB query mỗi request:

```java
orderRepository.existsByUserIdAndFlashSaleItemId(userId, flashSaleItemId)
```

Hai vấn đề:
1. **Hiệu năng**: Mỗi request đều query Postgres, tạo áp lực không cần thiết.
2. **Chuẩn bị cho async**: Nếu sau này save order qua RabbitMQ (async), order chưa có trong DB tại thời điểm check → duplicate check bằng DB sẽ bị sai.

## Giải pháp

Dùng Redis `SETNX` để check duplicate — atomic, nhanh, không cần DB:

```java
String purchasedKey = "purchased:" + flashSaleItemId + ":" + userId;
Boolean isNew = redisTemplate.opsForValue()
        .setIfAbsent(purchasedKey, "1", Duration.ofHours(24));

if (Boolean.FALSE.equals(isNew)) {
    throw new DuplicateOrderException();
}
```

- `SETNX` = check + set trong 1 lệnh, không bị race condition.
- TTL 24h (hoặc bằng thời gian flash sale) để tự cleanup.
- DB unique index `uniq_user_flashsale` vẫn giữ làm safety net cuối cùng.

Nếu các step sau fail (stock hết, publish fail, etc.), cần `DELETE` purchased key để user có thể retry.

## File cần sửa

- `src/main/java/com/flashsale/service/FlashSaleService.java`

## Độ ưu tiên

**Trung bình** — Tối ưu hiệu năng + bắt buộc nếu làm async (plan 04).
