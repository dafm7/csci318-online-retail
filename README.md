# CSCI318 Online Retail — AI-powered microservices

Spring Boot 3.4 · JDK 21 · Spring Data JPA + H2 · Spring Cloud Stream (Kafka + Kafka Streams) · LangChain4j (Gemini)

An online shop built as five Spring Boot microservices. Each service owns its own database. Services call each
other over REST when they need an answer straight away, and exchange domain events over Apache Kafka for the
order, payment and stock flow. Kafka Streams provides real-time analysis in the product, order and payment
services. Everything is used through REST endpoints; a React web shop and a desktop chat client are optional extras.

| Service | Port | Responsibility |
|---|---|---|
| `product-service` | 8081 | Catalogue, search and filters, admin add/restock, stock updates from orders, Kafka Streams: live stock, low-stock alerts, sales analytics |
| `user-service` | 8082 | Register, login, roles (CUSTOMER / ADMIN), profile with order summary |
| `order-service` | 8083 | Multi-item orders priced and stock-checked against product-service, order lifecycle (pay, ship, deliver, cancel, refund), Kafka Streams order analysis |
| `payment-service` | 8084 | Mock payments (up to $1000 succeeds), refunds, Kafka Streams payment analysis |
| `ai-agent-service` | 8085 | LangChain4j shopping assistant that uses the other services' APIs as tools |
| `frontend` (optional) | 5173 | React web shop and admin dashboard |

## Requirements

- **JDK 21** and **Maven 3.9+**
- **Apache Kafka** on `localhost:9092`
  - Mac: `brew install kafka && brew services start kafka`
  - Windows: install Kafka in WSL and run it on `localhost:9092`
- *Optional:* **Node 18+** for the web frontend
- *Optional:* a **Google Gemini API key** for the AI assistant (see below)

## Run

1. **Build** from the root folder:
   ```
   mvn clean package -DskipTests
   ```
2. **Start each service in its own terminal** (or run each `*Application` class from your IDE):
   ```
   mvn -pl user-service spring-boot:run
   mvn -pl product-service spring-boot:run
   mvn -pl order-service spring-boot:run
   mvn -pl payment-service spring-boot:run
   mvn -pl ai-agent-service spring-boot:run
   ```
   Wait until each shows `Started …Application`. The Kafka Streams parts need about 10 more seconds: until then
   the order and payment analysis endpoints return 503 ("stream is starting"), and `/api/inventory` and
   `/api/sales` return an error.
3. *Optional:* **web frontend** (needs Node 18+), then open http://localhost:5173:
   ```
   cd frontend
   npm install
   npm run dev
   ```
4. *Optional:* **desktop chat window**: `mvn -pl chat-client spring-boot:run`

### Accounts
- **Admin:** `admin@shop.com` / `admin123` (always user id **1**, created when user-service starts)
- **Customers:** register through `POST /api/users/register` or the web shop. The first customer is user id **2**.

### AI assistant (Gemini API key)
Only `ai-agent-service` (`/api/chat`) needs a key. Every other service and the frontend work without one; without
a key the assistant replies "Sorry, I can't answer right now".

1. Get a free key at https://aistudio.google.com/apikey
2. In the terminal that runs `ai-agent-service`, before starting it:
   - Mac/Linux: `export GEMINI_API_KEY=your-key`
   - Windows PowerShell: `$env:GEMINI_API_KEY="your-key"`

The assistant uses the model `gemini-3.8-flash`. If your key can't use that model (the `ai-agent-service` log shows
a model-not-found or permission error), choose another model from https://aistudio.google.com and set it too:
`export GEMINI_MODEL=model-name` (PowerShell: `$env:GEMINI_MODEL="model-name"`).

## Try it (REST)

Run these in order with the services running. Product ids 1–5 are the sample products created on startup
(1 Wireless Mouse $29.99, 2 Mechanical Keyboard $119, 3 Running Shoes $89.50, 4 Coffee Mug $12, 5 Hoodie $49.95).

**Users**
```
curl -X POST http://localhost:8082/api/users/register -H "Content-Type: application/json" -d '{"name":"Alice","email":"alice@uow.edu.au","password":"secret123"}'
curl -X POST http://localhost:8082/api/users/login -H "Content-Type: application/json" -d '{"email":"alice@uow.edu.au","password":"secret123"}'
curl http://localhost:8082/api/users/2                     # profile with order summary
```

**Products** (search, filter and sort can be combined)
```
curl http://localhost:8081/api/products
curl "http://localhost:8081/api/products?search=key"
curl "http://localhost:8081/api/products?category=electronics"     # CLOTHES, ELECTRONICS, SPORTS, HOME
curl "http://localhost:8081/api/products?minPrice=20&maxPrice=100&inStock=true&sort=price"   # sort: price, price_desc, name, name_desc, stock, stock_desc
curl http://localhost:8081/api/products/categories
```

**Admin** (send the admin's user id in `X-User-Id`; customers get 403)
```
curl -X POST http://localhost:8081/api/products -H "Content-Type: application/json" -H "X-User-Id: 1" -d '{"name":"Gaming Headset","category":"ELECTRONICS","price":79.95,"stock":15,"lowStockThreshold":5}'
curl -X POST http://localhost:8081/api/products/6/restock -H "Content-Type: application/json" -H "X-User-Id: 1" -d '{"quantity":10}'
curl http://localhost:8081/api/admin/products -H "X-User-Id: 1"   # all products grouped by category
```

**Order and pay** (payment is a separate step; the order is priced from product-service)
```
curl -X POST http://localhost:8083/api/orders -H "Content-Type: application/json" -d '{"customerId":2,"items":[{"productId":1,"quantity":2},{"productId":2,"quantity":1}]}'
curl -X POST http://localhost:8084/api/payments -H "Content-Type: application/json" -d '{"orderId":1,"paymentMethod":"mock-card"}'
curl http://localhost:8083/api/orders/1                    # PENDING, then PAID once the payment event arrives
curl "http://localhost:8083/api/orders?customerId=2"       # the customer's orders
curl http://localhost:8084/api/payments                    # all payments
```

**After payment**: either ship the order...
```
curl -X PATCH http://localhost:8083/api/orders/1/status -H "Content-Type: application/json" -d '{"status":"SHIPPED"}'
curl -X PATCH http://localhost:8083/api/orders/1/status -H "Content-Type: application/json" -d '{"status":"DELIVERED"}'
```
...or cancel it (PENDING, PAID or FAILED orders; stock is put back) and refund the payment:
```
curl -X POST "http://localhost:8083/api/orders/1/cancel?customerId=2"
curl -X POST http://localhost:8084/api/payments/orders/1/refund
```

**Real-time analysis (Kafka Streams)**
```
# Low-stock alert: 12 keyboards takes stock from 19 to 7, below its threshold of 10.
# (12 x $119 = $1428 is over the $1000 payment limit, so paying it would be declined: it is left unpaid here.)
curl -X POST http://localhost:8083/api/orders -H "Content-Type: application/json" -d '{"customerId":2,"items":[{"productId":2,"quantity":12}]}'
curl http://localhost:8081/api/inventory                   # live stock per product
curl http://localhost:8081/api/inventory/low-stock         # products below their own threshold
kafka-console-consumer --bootstrap-server localhost:9092 --topic low-stock-alerts --from-beginning

curl http://localhost:8081/api/sales                       # units sold, revenue, orders per product (paid orders only)
curl "http://localhost:8081/api/sales/per-minute?minutes=5"    # units sold per product, 1-minute tumbling windows
curl http://localhost:8083/api/orders/analysis/summary     # orders per status, cancellation rate, last minute (hopping window)
curl http://localhost:8084/api/payments/analysis/summary   # payments per status, success rate, amount paid, last minute
```

**AI assistant** (needs `GEMINI_API_KEY`)
```
curl -X POST http://localhost:8085/api/chat -H "Content-Type: application/json" -d '{"message":"Do you sell keyboards?"}'
# Send back the sessionId from the reply to continue the conversation; add customerId to ask about that customer's orders
curl -X POST http://localhost:8085/api/chat -H "Content-Type: application/json" -d '{"sessionId":"<sessionId>","customerId":2,"message":"What is the status of my latest order?"}'
curl -X DELETE http://localhost:8085/api/chat/<sessionId>  # forget the conversation
```

**Watch the events** (one terminal per topic)
```
kafka-console-consumer --bootstrap-server localhost:9092 --topic order-placed
kafka-console-consumer --bootstrap-server localhost:9092 --topic payment-completed
kafka-console-consumer --bootstrap-server localhost:9092 --topic order-cancelled
kafka-console-consumer --bootstrap-server localhost:9092 --topic stock-changed
```
Other topics: `payment-refunded`, `low-stock-alerts`, `order-analysis`, `payment-analysis`.

## Web frontend

`frontend/` is a Vite + React + TypeScript web shop:
- product browsing with search and categories
- a cart that places one order for all its items, then pays it
- sign in / register, and an account page with order history plus **Pay**, **Cancel** and **Refund**
- an admin dashboard (sign in as the admin): live inventory, best sellers, sales per minute, payments, all orders
  with **Ship**, **Deliver** and **Refund**, order and payment analysis, add product and restock
- the shop assistant as a chat panel

The dev server proxies each `/api/...` path to the right service (see `frontend/vite.config.ts`), so the backend
needs no CORS setup.

## Tests

```
mvn test
```
Includes the Kafka Streams analysis topologies, which run in memory with Kafka's `TopologyTestDriver` (no broker needed).

## Data and resetting

- **user-service and product-service** use in-memory H2 databases: they start empty on every restart (plus the
  admin account and the 5 sample products).
- **order-service and payment-service** save their H2 databases to files (`target/orderservice`,
  `target/paymentservice`, relative to the folder the service is started from: the module folder with
  `mvn -pl …`), so orders and payments survive a restart. `mvn clean` deletes them.
- **Kafka keeps all events and stream results on disk**, so the live stock table and sales totals still contain
  earlier runs. For a clean start, stop all services, then:
  ```
  kafka-topics --bootstrap-server localhost:9092 --list | grep -v '^__' | xargs -I{} kafka-topics --bootstrap-server localhost:9092 --delete --topic {}
  kafka-consumer-groups --bootstrap-server localhost:9092 --list | xargs -I{} kafka-consumer-groups --bootstrap-server localhost:9092 --delete --group {}
  rm -rf "$(java -XshowSettings:properties -version 2>&1 | awk -F'= ' '/java.io.tmpdir/ {print $2}')kafka-streams"
  mvn clean package -DskipTests
  ```
- H2 console: `http://localhost:<port>/h2-console` (JDBC URL is in each service's `application.yml`, user `sa`, no password).

## Troubleshooting

- **`Port … already in use`**: an older copy of a service is still running. Stop it (Ctrl+C in its terminal).
- **Only one copy of product-, order- or payment-service can run at a time** on one machine: Kafka Streams locks its local state folder.
- **Admin requests return 503**: product-service checks the admin role with user-service, so start user-service first.
- **Analysis, inventory or sales endpoints fail right after startup**: the Kafka Streams part is still starting; try again after a few seconds.
- **Red errors in VS Code after pulling** while `mvn clean package` succeeds: run *Java: Clean Java Language Server Workspace*.
