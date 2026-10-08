# CSCI318 Online Retail — AI-powered microservices

Spring Boot 3.4 · JDK 21 · Spring Data JPA + H2 · Spring Cloud Stream (Kafka) · LangChain4j (Gemini)

## Services

| Service | Port | Owns | Kafka |
|---|---|---|---|
| product-service | 8081 | products, stock | consumes `order-placed` → reduces stock |
| customer-service | 8082 | customer accounts | — |
| order-service | 8083 | orders | publishes `order-placed`, consumes `payment-completed` |
| payment-service | 8084 | payments | consumes `order-placed`, publishes `payment-completed` |
| ai-agent-service | 8085 | LangChain4j shopping/order assistant | — (calls other services over REST via `@Tool`) |
| frontend | — | HTML/CSS/JS | calls the REST APIs |

Event flow: `POST /api/orders` → **order-placed** → payment-service + product-service → **payment-completed** → order status becomes `PAID` / `FAILED`.

## Run locally

1. Start Kafka (needs Docker Desktop):
   ```
   docker compose up -d
   ```
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
   ```
5. Open `frontend/index.html` (VS Code Live Server works well).

## Quick test

```
curl http://localhost:8081/api/products
curl -X POST http://localhost:8082/api/customers -H "Content-Type: application/json" -d '{"name":"Alice","email":"alice@uow.edu.au","address":"Wollongong"}'
curl -X POST http://localhost:8083/api/orders -H "Content-Type: application/json" -d '{"customerId":1,"productId":1,"quantity":2,"amount":59.98}'
curl http://localhost:8083/api/orders/1      # status should become PAID
curl -X POST http://localhost:8085/api/chat -H "Content-Type: application/json" -d '{"message":"Do you sell keyboards?"}'
```

H2 console: `http://localhost:<port>/h2-console` (JDBC URL is in each service's `application.yml`, user `sa`, no password).

## Team workflow

- One person per service folder; don't edit other services' code without telling them.
- Work on a branch: `git checkout -b product/add-categories`, push, open a Pull Request into `main`.
- Event classes (`OrderPlacedEvent`, etc.) are copied into each service on purpose. If you change an event's fields, update every copy.
- Never commit API keys. Use the `GEMINI_API_KEY` environment variable.
