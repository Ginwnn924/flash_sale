# Flash Sale System – Extended Documentation

---

# 16. API Specification

## Base URL

```text
/api
```

---

## 16.1 Get Flash Sale Products

### Endpoint

```http
GET /api/flash-sale/products
```

### Description

Returns all products currently available in the flash sale.

### Response

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

### HTTP Status Codes

| Code | Meaning               |
| ---- | --------------------- |
| 200  | Success               |
| 500  | Internal server error |

---

## 16.2 Purchase Flash Sale Product

### Endpoint

```http
POST /api/flash-sale/purchase
```

### Request Body

```json
{
  "userId": 1001,
  "flashSaleItemId": 1
}
```

### Response

Success:

```json
{
  "orderId": 1044,
  "status": "SUCCESS"
}
```

Sold out:

```json
{
  "error": "SOLD_OUT"
}
```

Duplicate order:

```json
{
  "error": "DUPLICATE_ORDER"
}
```

---

# 17. Sequence Diagram – Purchase Flow

```id="xkzj1r"
User
 |
 | POST /purchase
 |
Load Balancer
 |
Spring Boot Controller
 |
FlashSaleService
 |
Redis DECR stock
 |
 |---- stock >= 0 ----|
 |                    |
Create Order         Sold Out
 |                    |
Insert DB             Return 409
 |
Return Success
```

### Steps

1. User sends purchase request
2. Load balancer routes request to an app instance
3. Application checks Redis stock
4. Redis performs atomic decrement
5. If stock is available → create order
6. Persist order in database
7. Return success response

---

# 18. Load Testing Strategy

Load testing is performed using **k6**.

Testing scenarios simulate:

* Massive traffic spike
* Flash sale purchase attempts
* High concurrency

Target metrics:

| Metric           | Target  |
| ---------------- | ------- |
| Throughput       | 20k RPS |
| Concurrent users | 50k     |
| P95 latency      | <40ms   |
| Error rate       | <0.1%   |

---

# 19. k6 Load Test Script

Example script to simulate flash sale purchase traffic.

```javascript
import http from "k6/http";
import { check } from "k6";

export const options = {
  vus: 5000,
  duration: "30s"
};

export default function () {

  const payload = JSON.stringify({
    userId: Math.floor(Math.random() * 100000),
    flashSaleItemId: 1
  });

  const params = {
    headers: {
      "Content-Type": "application/json"
    }
  };

  const res = http.post(
    "http://localhost:8080/api/flash-sale/purchase",
    payload,
    params
  );

  check(res, {
    "status is valid": (r) => r.status === 200 || r.status === 409
  });
}
```

### Run test

```bash
k6 run flashsale-test.js
```

---

# 20. Performance Metrics Example

Example output from load testing:

```
http_reqs:        600000
http_req_failed:  0.05%

latency
p(50)=12ms
p(95)=34ms
p(99)=60ms

throughput
20000 req/s
```

---

# 21. Monitoring

Production systems should include monitoring tools.

Recommended stack:

* Prometheus
* Grafana

Key metrics:

| Metric         | Description               |
| -------------- | ------------------------- |
| Request rate   | API traffic               |
| Latency        | Request processing time   |
| Error rate     | Failed requests           |
| Redis ops      | Stock decrement frequency |
| DB connections | Connection pool usage     |

---

# 22. Rate Limiting (Optional)

To protect the system during flash sale spikes.

Possible strategies:

* Token bucket
* Redis rate limiter
* API Gateway throttling

Example policy:

```
Max 10 purchase requests per user per minute
```

---

# 23. Future Improvements

Possible production-level improvements:

### Order Queue

Replace synchronous order creation with queue processing.

Architecture:

```
User
 |
API
 |
Redis stock check
 |
Kafka / RabbitMQ
 |
Order Worker
 |
Database
```

Benefits:

* Smoother DB writes
* Better spike handling

---

### Distributed Lock

Prevent multiple instances from processing the same order.

Possible implementation:

* Redis lock
* Redisson

---

### CDN Caching

Cache flash sale product lists to reduce backend load.

---

# 24. Project Goal

This project demonstrates:

* High-concurrency backend design
* Flash sale architecture
* Redis atomic operations
* Horizontal scalability
* Load testing and performance tuning
