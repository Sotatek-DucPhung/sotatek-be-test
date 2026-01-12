# Order Microservice

Order Service for the backend assessment. Handles order creation, retrieval, listing, and cancellation while integrating with Member, Product, and Payment services via an adapter pattern.

## Features

- Create orders with member/product/stock validation
- Payment processing and status progression: `PENDING` → `CONFIRMED` → `CANCELLED`
- Update endpoint only supports cancelling confirmed orders
- External services mocked by default, switchable to real HTTP clients
- Resilience4j circuit breaker + retry on external calls
- Flyway database migrations
- Swagger/OpenAPI at `/swagger-ui.html`

## Endpoints

- `POST /api/orders` create order
- `GET /api/orders/{id}` get order
- `GET /api/orders` list orders with pagination/filter
- `PUT /api/orders/{id}` cancel order (status only, `CONFIRMED` → `CANCELLED`)

## Database Migrations (Flyway)

Migrations live in `src/main/resources/db/migration`. They run automatically on startup.

## External Service Mocks

Mocks are enabled by default.

Environment variables:
- `EXTERNAL_MOCK_ENABLED` (default `true`)
- `MEMBER_SERVICE_URL` (default `http://localhost:8081`)
- `PRODUCT_SERVICE_URL` (default `http://localhost:8082`)
- `PAYMENT_SERVICE_URL` (default `http://localhost:8083`)

## How to Run

### Local (Gradle)
```
./gradlew bootRun
```

### Docker
```
docker compose up -d --build
```

App runs at `http://localhost:8080` and Postgres at `localhost:5433`.

## Testing

Unit + controller tests:
```
./gradlew clean test
```

Integration/e2e tests:
```
./gradlew clean e2eTest
```

All verification:
```
./gradlew check
```

## Design Notes

- Layered architecture: controller → service → repository
- Adapter pattern for external services (mock vs real)
- Centralized error handling via `GlobalExceptionHandler`
- Status updates restricted to cancellation only, per requirements
