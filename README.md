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
   mvn -pl customer-service spring-boot:run
   mvn -pl order-service spring-boot:run
   mvn -pl payment-service spring-boot:run
   mvn -pl ai-agent-service spring-boot:run
   mvn -pl chat-client spring-boot:run      # optional: desktop chat window
   ```

## Quick test

```
curl http://localhost:8081/api/products
curl "http://localhost:8081/api/products?search=key"
curl "http://localhost:8081/api/products?category=Electronics"
curl -X POST http://localhost:8081/api/products -H "Content-Type: application/json" -d '{"name":"Gaming Headset","category":"Electronics","price":79.95,"stock":15}'
curl -X POST http://localhost:8082/api/customers/register -H "Content-Type: application/json" -d '{"name":"Alice","email":"alice@uow.edu.au","password":"secret123","address":"Wollongong"}'
curl -X POST http://localhost:8082/api/customers/login -H "Content-Type: application/json" -d '{"email":"alice@uow.edu.au","password":"secret123"}'
curl http://localhost:8082/api/customers/1      # view profile
curl -X POST http://localhost:8083/api/orders -H "Content-Type: application/json" -d '{"customerId":1,"productId":1,"quantity":2,"amount":59.98}'
curl http://localhost:8083/api/orders/1      # status should become PAID
curl -X POST http://localhost:8085/api/chat -H "Content-Type: application/json" -d '{"message":"Do you sell keyboards?"}'
# Keep talking in the same conversation by sending back the sessionId from the reply.
# Add the customerId from login so the assistant can answer "where is my order?" for that customer only.
curl -X POST http://localhost:8085/api/chat -H "Content-Type: application/json" -d '{"sessionId":"<sessionId from the reply>","customerId":1,"message":"What is the status of my latest order?"}'
curl -X DELETE http://localhost:8085/api/chat/<sessionId>      # forget a conversation

# Real-time inventory (Kafka Streams) - order 12 keyboards (stock 20) to trigger a low-stock alert
curl -X POST http://localhost:8083/api/orders -H "Content-Type: application/json" -d '{"customerId":1,"productId":2,"quantity":12,"amount":900}'
curl http://localhost:8081/api/inventory              # live stock per product
curl http://localhost:8081/api/inventory/low-stock    # products below the threshold (10)
kafka-console-consumer --bootstrap-server localhost:9092 --topic low-stock-alerts --from-beginning

# Real-time sales analysis (Kafka Streams join + aggregation + 1-minute windows, paid orders only)
curl http://localhost:8081/api/sales                       # units sold, revenue, orders per product
curl "http://localhost:8081/api/sales/per-minute?minutes=5" # units sold per product per minute
```

## Chat with the shop assistant (desktop window)

`chat-client` is a small Java Swing app for talking to the assistant. With the services running, start it with
`mvn -pl chat-client spring-boot:run` (or run `ChatClientApplication` from IntelliJ).
Sign in with a customer you registered (e.g. alice@uow.edu.au / secret123) to ask about your own orders.
It only uses the JDK and Jackson, so it adds no new dependencies.

H2 console: `http://localhost:<port>/h2-console` (JDBC URL is in each service's `application.yml`, user `sa`, no password).
