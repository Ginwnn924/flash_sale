# Flash Sale System – Backend Requirement

## 1. Overview

This project implements a **high-concurrency flash sale backend system** designed to simulate real-world e-commerce flash sale scenarios.

The system focuses on:

* Handling **high traffic spikes**
* Preventing **overselling**
* Supporting **horizontal scaling**
* Achieving **low latency and high throughput**

The implementation is based on:

* Java
* Spring Boot
* Redis
* PostgreSQL

---

# 2. System Architecture

```
             CDN
              |
           Nginx LB
          /   |   \
     App1   App2   App3
        \     |     /
           Redis
     (cache + stock)
             |
         PostgreSQL
```

## Components

### Load Balancer

Distributes incoming requests to multiple backend instances.

### Application Servers

Stateless Spring Boot services that handle business logic.

### Redis

Used for:

* Product cache
* Flash sale stock control
* High-speed atomic operations

### PostgreSQL

Persistent storage for:

* Products
* Flash sale events
* Orders

---

# 3. Flash Sale Order Flow

```
User Request
     |
Load Balancer
     |
Spring Boot API
     |
Redis DECR stock
     |
if stock >= 0
     |
Create Order
     |
Persist to DB
```

If stock < 0 → return **SOLD_OUT** response.

---

# 4. Exception Handling Strategy

Centralized exception handling using:

```
@ControllerAdvice
```

### Custom Exceptions

#### SoldOutException

Thrown when stock is unavailable.

#### DuplicateOrderException

Thrown when a user attempts to buy the same flash sale item twice.

#### FlashSaleNotActiveException

Thrown when flash sale has not started or has ended.

---

### Global Exception Handler

Example responses:

| Error                 | HTTP Code |
| --------------------- | --------- |
| SOLD_OUT              | 409       |
| DUPLICATE_ORDER       | 409       |
| FLASH_SALE_NOT_ACTIVE | 400       |
| INTERNAL_ERROR        | 500       |

---

# 5. Package Structure

```
flashsale
 ├── controller
 ├── service
 ├── repository
 ├── entity
 ├── dto
 ├── mapper
 ├── exception
 └── config
```

---

# 6. Controllers

## FlashSaleController

Handles flash sale APIs.

### Endpoints

#### Get Flash Sale Products

```
GET /api/flash-sale/products
```

Response:

```
[
  {
    "productId": 1,
    "model": "iPhone 15",
    "flashPrice": 15990000,
    "stock": 100
  }
]
```

---

#### Purchase Flash Sale Product

```
POST /api/flash-sale/purchase
```

Request:

```
{
  "userId": 1001,
  "flashSaleItemId": 1
}
```

Response:

```
{
  "orderId": 123,
  "status": "SUCCESS"
}
```

---

# 7. Services

## FlashSaleService

Responsibilities:

* Validate flash sale time
* Check duplicate orders
* Decrement Redis stock
* Create order record

### Key Methods

```
purchaseFlashSaleItem(userId, flashSaleItemId)
```

```
getFlashSaleProducts()
```

---

# 8. Entities

## Product

```
id
brand
model
storage
color
price
created_at
```

---

## FlashSaleItem

```
id
product_id
flash_price
total_stock
start_time
end_time
```

---

## Order

```
id
user_id
flash_sale_item_id
product_id
product_name
price
status
created_at
```

Order status:

```
0 = pending
1 = paid
2 = cancelled
```

---

# 9. DTOs

## PurchaseRequestDTO

```
userId
flashSaleItemId
```

---

## FlashSaleProductDTO

```
productId
model
flashPrice
stock
```

---

## OrderResponseDTO

```
orderId
status
```

---

# 10. Mapper

Mapping between:

```
Entity ↔ DTO
```

Example:

```
FlashSaleItem → FlashSaleProductDTO
```

Responsibilities:

* Convert database entity to API response
* Avoid exposing internal fields

Example method:

```
FlashSaleProductDTO toDTO(FlashSaleItem entity)
```

---

# 11. Database Schema

## Products

```
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    brand VARCHAR(50),
    model VARCHAR(100),
    storage VARCHAR(20),
    color VARCHAR(30),
    price BIGINT,
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## Flash Sale Items

```
CREATE TABLE flash_sale_items (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT,
    flash_price BIGINT,
    total_stock INT,
    start_time TIMESTAMP,
    end_time TIMESTAMP
);
```

---

## Orders

```
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    flash_sale_item_id BIGINT,
    product_id BIGINT,
    product_name VARCHAR(150),
    price BIGINT,
    status SMALLINT,
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

# 12. Indexes

```
CREATE INDEX idx_orders_user
ON orders(user_id);

CREATE INDEX idx_orders_flashsale
ON orders(flash_sale_item_id);
```

Prevent duplicate orders:

```
CREATE UNIQUE INDEX uniq_user_flashsale
ON orders(user_id, flash_sale_item_id);
```

---

# 13. Redis Key Design

Stock keys:

```
stock:flashsale:1
stock:flashsale:2
stock:flashsale:3
```

Example value:

```
stock:flashsale:1 = 100
```

Stock operation:

```
DECR stock:flashsale:{id}
```

---

# 14. Load Testing

Tool used:

* k6

Metrics target:

| Metric           | Target  |
| ---------------- | ------- |
| Throughput       | 20k RPS |
| Concurrent users | 50k     |
| P95 latency      | <40ms   |
| Error rate       | <0.1%   |

---

# 15. Future Improvements

Possible improvements include:

* Kafka queue for order processing
* Rate limiting
* Distributed locking
* Monitoring with Prometheus + Grafana

---
