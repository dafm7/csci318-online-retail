# CSCI318 Online Retail — AI-powered microservices

Spring Boot 3.4 · JDK 21 · Spring Data JPA + H2 · Spring Cloud Stream (Kafka) · LangChain4j (Gemini)

## Run locally

1. Start Kafka:
   - Mac: brew install kafka && brew services start kafka
   - Windows: install Kafka in WSL and run it on localhost:9092
2. Set your Gemini API key (free from https://aistudio.google.com/apikey):
   - Mac/Linux: `export GEMINI_API_KEY=your-key`
   - Windows PowerShell: `$env:GEMINI_API_KEY="your-key"`
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
curl -X POST http://localhost:8083/api/orders -H "Content-Type: application/json" -d '{"customerId":2,"productId":1,"quantity":2,"amount":59.98}'
curl http://localhost:8083/api/orders/1      # status should become PAID
curl -X POST http://localhost:8085/api/chat -H "Content-Type: application/json" -d '{"message":"Do you sell keyboards?"}'

# Real-time inventory (Kafka Streams) - order 12 keyboards (stock 20) to trigger a low-stock alert
curl -X POST http://localhost:8083/api/orders -H "Content-Type: application/json" -d '{"customerId":2,"productId":2,"quantity":12,"amount":900}'
curl http://localhost:8081/api/inventory              # live stock per product
curl http://localhost:8081/api/inventory/low-stock    # products below the threshold (10)
kafka-console-consumer --bootstrap-server localhost:9092 --topic low-stock-alerts --from-beginning

# Real-time sales analysis (Kafka Streams join + aggregation + 1-minute windows, paid orders only)
curl http://localhost:8081/api/sales                       # units sold, revenue, orders per product
curl "http://localhost:8081/api/sales/per-minute?minutes=5" # units sold per product per minute
```

H2 console: `http://localhost:<port>/h2-console` (JDBC URL is in each service's `application.yml`, user `sa`, no password).
