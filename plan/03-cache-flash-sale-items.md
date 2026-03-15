# 03 - Cache Flash Sale Items

## Vấn đề

Mỗi request mua hàng đều `findById` vào Postgres để lấy `FlashSaleItem`:

```java
FlashSaleItem flashSaleItem = flashSaleItemRepository.findById(flashSaleItemId)
        .orElseThrow(...);
```

Data flash sale item hiếm khi thay đổi trong suốt thời gian sale, nhưng mỗi request đều query DB — lãng phí dưới tải cao.

## Giải pháp

Cache flash sale items trong Redis hoặc local cache (Caffeine).

### Option A: Local cache (Caffeine) — đơn giản, đủ dùng cho single instance

```java
@Cacheable(value = "flashSaleItems", key = "#id")
public FlashSaleItem getFlashSaleItem(Long id) {
    return flashSaleItemRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("..."));
}
```

Config Caffeine TTL = thời gian flash sale hoặc 5-10 phút.

### Option B: Redis cache — cần thiết nếu chạy multiple instances

Serialize `FlashSaleItem` (kèm `Product` info) vào Redis khi warm-up, đọc từ Redis thay vì DB.

Có thể mở rộng `warmUpStock()` để cache luôn flash sale item data.

## File cần sửa

- `src/main/java/com/flashsale/service/FlashSaleService.java`
- `pom.xml` (thêm Caffeine nếu dùng Option A)
- `src/main/resources/application.yml` (cache config)

## Độ ưu tiên

**Thấp** — Tối ưu hiệu năng, không ảnh hưởng tính đúng đắn. Làm sau khi fix bug.
