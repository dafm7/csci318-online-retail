# CSCI318 Online Retail — AI-powered microservices

Spring Boot 3.4 · JDK 21 · Spring Data JPA + H2 · Spring Cloud Stream (Kafka) · LangChain4j (Gemini)

## Run locally

1. Start Kafka:
   - Mac: brew install kafka && brew services start kafka
   - Windows: install Kafka in WSL and run it on localhost:9092
2. Set your Gemini API key (free from https://aistudio.google.com/apikey), in each terminal that runs `ai-agent-service`:
   - Mac/Linux: `export GEMINI_API_KEY=your-key`
   - Windows PowerShell: `$env:GEMINI_API_KEY="your-key"`

   The key is only needed for the AI shop assistant (`ai-agent-service`, `/api/chat`). Every other service and the
   frontend work without it; without a key the assistant replies "Sorry, I can't answer right now".

   The assistant uses the model `gemini-3.8-flash`. If your key can't use that model (the `ai-agent-service` log
   shows a model-not-found or permission error), pick another model from https://aistudio.google.com and set it
   before starting the service:
   - Mac/Linux: `export GEMINI_MODEL=model-name`
   - Windows PowerShell: `$env:GEMINI_MODEL="model-name"`
3. Build everything from the root folder:
   ```
   mvn clean package -DskipTests
   ```
4. Run each service in its own terminal (or run each `*Application` class from IntelliJ):
   ```
   mvn -pl product-service spring-boot:run
   mvn -pl user-service spring-boot:run
   mvn -pl order-service spring-boot:run
   mvn -pl payment-service spring-boot:run
   mvn -pl ai-agent-service spring-boot:run
   mvn -pl chat-client spring-boot:run      # optional: desktop chat window
   ```

## Quick test

```
curl http://localhost:8081/api/products
curl "http://localhost:8081/api/products?search=key"
curl "http://localhost:8081/api/products?category=electronics"   # CLOTHES, ELECTRONICS, SPORTS, HOME
curl "http://localhost:8081/api/products?minPrice=20&maxPrice=100&inStock=true&sort=price"   # sort: price, price_desc, name, name_desc, stock, stock_desc
curl http://localhost:8081/api/products/categories
# Admin only (user 1 = admin@shop.com, created on startup). Send the admin's id in X-User-Id
curl -X POST http://localhost:8081/api/products -H "Content-Type: application/json" -H "X-User-Id: 1" -d '{"name":"Gaming Headset","category":"ELECTRONICS","price":79.95,"stock":15}'
curl http://localhost:8081/api/admin/products -H "X-User-Id: 1"   # all products grouped by category
curl -X POST http://localhost:8082/api/users/register -H "Content-Type: application/json" -d '{"name":"Alice","email":"alice@uow.edu.au","password":"secret123"}'
curl -X POST http://localhost:8082/api/users/login -H "Content-Type: application/json" -d '{"email":"alice@uow.edu.au","password":"secret123"}'
curl http://localhost:8082/api/users/2          # view profile (user 1 is the admin)
curl -X POST http://localhost:8083/api/orders -H "Content-Type: application/json" -d '{"customerId":2,"items":[{"productId":1,"quantity":2},{"productId":2,"quantity":1}]}'
curl -X POST http://localhost:8084/api/payments -H "Content-Type: application/json" -d '{"orderId":1,"paymentMethod":"mock-card"}'
curl http://localhost:8083/api/orders/1      # status becomes PAID after the payment event
# Then EITHER ship it (PAID -> SHIPPED -> DELIVERED)...
curl -X PATCH http://localhost:8083/api/orders/1/status -H "Content-Type: application/json" -d '{"status":"SHIPPED"}'
# ...OR cancel it (PENDING, PAID or FAILED orders; stock is put back) and refund the payment
curl -X POST "http://localhost:8083/api/orders/1/cancel?customerId=2"
curl -X POST http://localhost:8084/api/payments/orders/1/refund
curl http://localhost:8083/api/orders/analysis/summary
curl http://localhost:8084/api/payments/analysis/summary
curl -X POST http://localhost:8085/api/chat -H "Content-Type: application/json" -d '{"message":"Do you sell keyboards?"}'
# Keep talking in the same conversation by sending back the sessionId from the reply.
# Add the customerId from login so the assistant can answer "where is my order?" for that customer only.
curl -X POST http://localhost:8085/api/chat -H "Content-Type: application/json" -d '{"sessionId":"<sessionId from the reply>","customerId":2,"message":"What is the status of my latest order?"}'
curl -X DELETE http://localhost:8085/api/chat/<sessionId>      # forget a conversation

# Real-time inventory (Kafka Streams) - order 12 keyboards (stock 20) to trigger a low-stock alert
curl -X POST http://localhost:8083/api/orders -H "Content-Type: application/json" -d '{"customerId":2,"items":[{"productId":2,"quantity":12}]}'
curl -X POST http://localhost:8084/api/payments -H "Content-Type: application/json" -d '{"orderId":2,"paymentMethod":"mock-card"}'   # pay it so it counts in /api/sales
curl http://localhost:8081/api/inventory              # live stock per product
curl http://localhost:8081/api/inventory/low-stock    # products below the threshold (10)
kafka-console-consumer --bootstrap-server localhost:9092 --topic low-stock-alerts --from-beginning

# Real-time sales analysis (Kafka Streams join + aggregation + 1-minute windows, paid orders only)
curl http://localhost:8081/api/sales                       # units sold, revenue, orders per product
curl "http://localhost:8081/api/sales/per-minute?minutes=5" # units sold per product per minute
```

## Web frontend (React)

`frontend/` is a Vite + React + TypeScript web shop: product browsing, cart and checkout, sign in / register,
account page with order history, an admin dashboard (live inventory, sales, payments, add product) and the
shop assistant as a chat panel. Needs Node 18+. With the services running:

```
cd frontend
npm install
npm run dev        # http://localhost:5173
```

The dev server proxies each `/api/...` path to the right service port (see `frontend/vite.config.ts`),
so no CORS setup is needed on the backend. Sign in as `admin@shop.com` / `admin123` to open the admin dashboard.

## Chat with the shop assistant (desktop window)

`chat-client` is a small Java Swing app for talking to the assistant. With the services running, start it with
`mvn -pl chat-client spring-boot:run` (or run `ChatClientApplication` from IntelliJ).
Sign in with a customer you registered (e.g. alice@uow.edu.au / secret123) to ask about your own orders.
It only uses the JDK and Jackson, so it adds no new dependencies.

H2 console: `http://localhost:<port>/h2-console` (JDBC URL is in each service's `application.yml`, user `sa`, no password).
