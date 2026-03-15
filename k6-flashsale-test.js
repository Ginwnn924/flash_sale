import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Trend } from "k6/metrics";

// ── Custom Metrics ──────────────────────────
const purchaseSuccess = new Counter("purchase_success");
const purchaseSoldOut = new Counter("purchase_sold_out");
const purchaseDuplicate = new Counter("purchase_duplicate");
const browseDuration = new Trend("browse_duration", true);
const purchaseDuration = new Trend("purchase_duration", true);

// ── Config ──────────────────────────────────
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const MAX_USER_ID = parseInt(__ENV.MAX_USER_ID || "100000");

// ── Load Profile ────────────────────────────
export const options = {
  stages: [
    { duration: "10s", target: 500 },   // ramp-up
    { duration: "30s", target: 2000 },  // peak traffic
    { duration: "10s", target: 0 },     // ramp-down
  ],
  thresholds: {
    http_req_duration: ["p(95)<500"],
    browse_duration: ["p(95)<300"],
    purchase_duration: ["p(95)<500"],
  },
};

// ── User Flow ───────────────────────────────
export default function () {
  const headers = { "Content-Type": "application/json" };

  // Step 1: Browse flash sale products
  const browseRes = http.get(`${BASE_URL}/api/flash-sale/products`, {
    tags: { name: "GET /products" },
  });
  browseDuration.add(browseRes.timings.duration);

  const browseOk = check(browseRes, {
    "browse: status 200": (r) => r.status === 200,
    "browse: has products": (r) => JSON.parse(r.body).length > 0,
  });

  if (!browseOk) return;

  // User thinks... scrolling through products
  sleep(Math.random() * 2 + 1); // 1-3s

  // Step 2: Pick a random product and purchase
  const products = JSON.parse(browseRes.body);
  const picked = products[Math.floor(Math.random() * products.length)];

  const payload = JSON.stringify({
    userId: Math.floor(Math.random() * MAX_USER_ID) + 1,
    flashSaleItemId: picked.productId,
  });

  const purchaseRes = http.post(
    `${BASE_URL}/api/flash-sale/purchase`,
    payload,
    { headers, tags: { name: "POST /purchase" }, responseCallback: http.expectedStatuses(200, 409) }
  );
  purchaseDuration.add(purchaseRes.timings.duration);

  if (purchaseRes.status === 200) {
    purchaseSuccess.add(1);
    check(purchaseRes, {
      "purchase: has orderId": (r) => JSON.parse(r.body).orderId !== undefined,
    });
  } else if (purchaseRes.status === 409) {
    const body = JSON.parse(purchaseRes.body);
    if (body.error === "SOLD_OUT") purchaseSoldOut.add(1);
    if (body.error === "DUPLICATE_ORDER") purchaseDuplicate.add(1);
  }

  sleep(Math.random() * 0.5); // micro pause before next iteration
}
