# ⚡ Flash Sale System

A **high-concurrency flash sale backend** built with **Spring Boot 4**, **Redis**, and **PostgreSQL** — designed to handle massive traffic spikes, prevent overselling, and maintain low latency under heavy load.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Key Features](#key-features)
- [Purchase Flow](#purchase-flow)
- [API Endpoints](#api-endpoints)
- [Database Schema](#database-schema)
- [Getting Started](#getting-started)
- [Load Testing](#load-testing)
- [Project Structure](#project-structure)

---

## Overview

Flash sales generate extreme traffic spikes — thousands of users competing for limited stock within seconds. This project simulates that scenario and solves the core backend challenges:

- **Race conditions** — multiple users trying to buy the last item simultaneously
- **Overselling prevention** — ensuring stock never goes negative
- **Horizontal scalability** — stateless design that works behind a load balancer
- **Low latency** — sub-40ms P95 response time under 2,000 concurrent users

---

## Architecture

```
              CDN (static assets)
                    │
               Nginx / LB
              ╱     │     ╲
         App 1    App 2    App 3      ← Stateless Spring Boot instances
              ╲     │     ╱
               Redis 7
         (stock cache + atomic ops)
                    │
             PostgreSQL 16
          (orders + products)
```

- **Load Balancer** distributes traffic across multiple app instances
- **Redis** handles stock management with atomic `DECR` — the hot path never touches the DB
- **PostgreSQL** persists orders and product catalog — the cold path

---

## Tech Stack

| Layer             | Technology                             |
| ----------------- | -------------------------------------- |
| **Runtime**       | Java 21                                |
| **Framework**     | Spring Boot 4.0.3                      |
| **Database**      | PostgreSQL 16 (Alpine)                 |
| **Cache**         | Redis 7 (Alpine)                       |
| **ORM**           | Spring Data JPA + Hibernate            |
| **Mapping**       | MapStruct 1.6.3                        |
| **Validation**    | Jakarta Bean Validation                |
| **Containerization** | Docker Compose                      |
| **Load Testing**  | k6                                     |

---

## Key Features

### 🔒 Atomic Stock Decrement with Redis
Stock is pre-loaded into Redis at warm-up. Purchase requests use `DECR` for atomic stock checks — no locks, no race conditions, single-digit millisecond latency.

### 🛡️ Overselling Prevention
If `DECR` returns a negative value, the stock is immediately rolled back with `INCR` and a `SOLD_OUT` error is returned. The database is never touched for a failed purchase.

### 🚫 Duplicate Order Prevention
A unique constraint (`user_id`, `flash_sale_item_id`) at both the application layer and database layer ensures one purchase per user per item.

### ⏰ Time-Window Validation
Flash sale items have `start_time` and `end_time`. Purchases outside the active window are rejected with `FLASH_SALE_NOT_ACTIVE`.

### 🔥 Redis Stock Warm-Up
A dedicated endpoint loads current stock counts from PostgreSQL into Redis before the sale begins, ensuring cache consistency.

### 🎯 Centralized Exception Handling
`@RestControllerAdvice` maps domain exceptions to clean, structured JSON error responses with proper HTTP status codes.

---

## Purchase Flow

```
User Request
     │
     ▼
┌─────────────┐
│  Controller  │  ← Validate request body
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Service    │  ← Check flash sale is active
│              │  ← Check duplicate order
└──────┬──────┘
       │
       ▼
┌─────────────┐
│    Redis     │  ← DECR stock:flashsale:{id}
│              │  ← if < 0 → INCR (rollback) → SOLD_OUT
└──────┬──────┘
       │ stock ≥ 0
       ▼
┌─────────────┐
│ PostgreSQL   │  ← INSERT order
└──────┬──────┘
       │
       ▼
   Response: { orderId, status: "SUCCESS" }
```

---

## API Endpoints

### `GET /api/flash-sale/products`

Returns all currently active flash sale products with real-time stock from Redis.

**Response** `200 OK`
```json
[
  {
    "productId": 1,
    "brand": "Apple",
    "model": "iPhone 15",
    "storage": "128GB",
    "color": "Black",
    "flashPrice": 15990000,
    "remainingStock": 87
  }
]
```

---

### `POST /api/flash-sale/purchase`

Attempt to purchase a flash sale item.

**Request Body**
```json
{
  "userId": 1001,
  "flashSaleItemId": 1
}
```

**Response** `200 OK`
```json
{
  "orderId": 1044,
  "status": "SUCCESS"
}
```

**Error Responses**

| HTTP Code | Error                  | Cause                              |
| --------- | ---------------------- | ---------------------------------- |
| 409       | `SOLD_OUT`             | Stock depleted                     |
| 409       | `DUPLICATE_ORDER`      | User already purchased this item   |
| 400       | `FLASH_SALE_NOT_ACTIVE`| Outside sale time window           |
| 400       | `VALIDATION_ERROR`     | Invalid request body               |

---

### `POST /api/flash-sale/warm-up`

Loads stock from PostgreSQL into Redis. Call this before the sale starts.

**Response** `200 OK`
```json
{
  "status": "SUCCESS",
  "itemsLoaded": 5
}
```

---

## Database Schema

### Products
```sql
CREATE TABLE products (
    id         BIGSERIAL PRIMARY KEY,
    brand      VARCHAR(50)  NOT NULL,
    model      VARCHAR(100) NOT NULL,
    storage    VARCHAR(20),
    color      VARCHAR(30),
    price      BIGINT       NOT NULL,
    created_at TIMESTAMP    DEFAULT NOW()
);
```

### Flash Sale Items
```sql
CREATE TABLE flash_sale_items (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT    NOT NULL REFERENCES products(id),
    flash_price BIGINT    NOT NULL,
    total_stock INT       NOT NULL,
    start_time  TIMESTAMP NOT NULL,
    end_time    TIMESTAMP NOT NULL
);
```

### Orders
```sql
CREATE TABLE orders (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT       NOT NULL,
    flash_sale_item_id BIGINT       NOT NULL REFERENCES flash_sale_items(id),
    product_id         BIGINT       NOT NULL REFERENCES products(id),
    product_name       VARCHAR(150) NOT NULL,
    price              BIGINT       NOT NULL,
    status             SMALLINT     NOT NULL DEFAULT 0,
    created_at         TIMESTAMP    DEFAULT NOW()
);

-- Performance indexes
CREATE INDEX idx_orders_user       ON orders(user_id);
CREATE INDEX idx_orders_flashsale  ON orders(flash_sale_item_id);
CREATE UNIQUE INDEX uniq_user_flashsale ON orders(user_id, flash_sale_item_id);
```

---

## Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### 1. Start Infrastructure

```bash
docker compose up -d
```

This spins up **PostgreSQL 16** (port 5432) and **Redis 7** (port 6379).

### 2. Run the Application

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. Schema and seed data are auto-loaded on first run.

### 3. Warm Up Redis Stock

```bash
curl -X POST http://localhost:8080/api/flash-sale/warm-up
```

### 4. Test a Purchase

```bash
curl -X POST http://localhost:8080/api/flash-sale/purchase \
  -H "Content-Type: application/json" \
  -d '{"userId": 1001, "flashSaleItemId": 1}'
```

---

## Load Testing

Load tests are written with [k6](https://k6.io/) to simulate realistic flash sale traffic.

### Run the test

```bash
k6 run k6-flashsale-test.js
```

### Test Profile

| Phase     | Duration | Virtual Users |
| --------- | -------- | ------------- |
| Ramp-up   | 10s      | 0 → 500       |
| Peak      | 30s      | 500 → 2,000   |
| Ramp-down | 10s      | 2,000 → 0     |

### Target Metrics

| Metric              | Threshold  |
| ------------------- | ---------- |
| P95 Latency         | < 500ms    |
| HTTP Failure Rate   | < 5%       |
| Browse P95          | < 300ms    |
| Purchase P95        | < 500ms    |

---

## Project Structure

```
src/main/java/com/flashsale/
├── FlashSaleApplication.java          # Entry point
├── config/
│   └── RedisConfig.java               # Redis template bean
├── controller/
│   └── FlashSaleController.java       # REST endpoints
├── dto/
│   ├── ErrorResponseDTO.java          # Error response structure
│   ├── FlashSaleProductDTO.java       # Product listing DTO
│   ├── OrderResponseDTO.java          # Purchase response DTO
│   └── PurchaseRequestDTO.java        # Purchase request DTO
├── entity/
│   ├── FlashSaleItem.java             # Flash sale item entity
│   ├── Order.java                     # Order entity
│   └── Product.java                   # Product entity
├── enums/
│   └── OrderStatus.java               # PENDING / PAID / CANCELLED
├── exception/
│   ├── DuplicateOrderException.java   # Duplicate purchase guard
│   ├── FlashSaleNotActiveException.java
│   ├── GlobalExceptionHandler.java    # Centralized error handling
│   └── SoldOutException.java          # Stock depleted
├── mapper/
│   └── FlashSaleMapper.java           # MapStruct entity ↔ DTO
├── repository/
│   ├── FlashSaleItemRepository.java
│   ├── OrderRepository.java
│   └── ProductRepository.java
└── service/
    └── FlashSaleService.java          # Core business logic
```

---

## License

This project is for educational and portfolio purposes.