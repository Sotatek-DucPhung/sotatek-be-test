# Order Microservice - Technical Documentation

**Version:** 1.0.0
**Date:** January 2026
**Author:** Backend Development Team
**Status:** Implementation Ready

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Architecture Design](#2-architecture-design)
3. [Technology Stack](#3-technology-stack)
4. [Data Model](#4-data-model)
5. [API Specification](#5-api-specification)
6. [External Service Integration](#6-external-service-integration)
7. [Error Handling Strategy](#7-error-handling-strategy)
8. [Security Considerations](#8-security-considerations)
9. [Testing Strategy](#9-testing-strategy)
10. [Deployment Guide](#10-deployment-guide)
11. [Monitoring and Logging](#11-monitoring-and-logging)
12. [Development Guidelines](#12-development-guidelines)

---

## 1. System Overview

### 1.1 Purpose

The Order Microservice is a RESTful service responsible for managing the complete lifecycle of customer orders in a distributed e-commerce system. It orchestrates interactions with external services (Member, Product, and Payment services) to ensure order validity and payment processing.

### 1.2 Scope

**In Scope:**
- Order CRUD operations (Create, Read, Update, Delete)
- Order validation through external service integration
- Payment processing orchestration
- Order status management and state transitions
- Pagination and filtering for order listings
- Resilient external service communication with Circuit Breaker and Retry patterns

**Out of Scope:**
- User authentication and authorization (handled by API Gateway)
- Inventory management (handled by Product Service)
- Payment processing logic (handled by Payment Service)
- Shipping and fulfillment (future service)
- Order notifications (future service)

### 1.3 System Context

```
┌──────────────────────────────────────────────────────────────┐
│                     External Services                         │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐   │
│  │    Member     │  │    Product    │  │    Payment    │   │
│  │   Service     │  │   Service     │  │   Service     │   │
│  │  (Port 8081)  │  │  (Port 8082)  │  │  (Port 8083)  │   │
│  └───────┬───────┘  └───────┬───────┘  └───────┬───────┘   │
└──────────┼──────────────────┼──────────────────┼───────────┘
           │                  │                  │
           │   HTTP/REST      │                  │
           │   (Circuit       │                  │
           │    Breaker)      │                  │
           │                  │                  │
    ┌──────┴──────────────────┴──────────────────┴──────┐
    │                                                     │
    │              Order Microservice                     │
    │                                                     │
    │  ┌─────────────┐  ┌──────────────┐  ┌──────────┐ │
    │  │ Controller  │  │   Service    │  │Repository│ │
    │  │   Layer     │→ │    Layer     │→ │  Layer   │ │
    │  └─────────────┘  └──────────────┘  └────┬─────┘ │
    │                                           │        │
    │                   (Port 8080)             │        │
    └───────────────────────────────────────────┼────────┘
                                                │
                                                │ JDBC
                                                ↓
                                    ┌────────────────────┐
                                    │   PostgreSQL DB    │
                                    │   (Port 5432)      │
                                    └────────────────────┘
```

---

## 2. Architecture Design

### 2.1 Layered Architecture

The service follows a clean layered architecture pattern:

```
┌─────────────────────────────────────────────────────────┐
│                   Presentation Layer                     │
│  ┌──────────────┐         ┌──────────────────────────┐ │
│  │ REST         │         │ Exception Handler        │ │
│  │ Controllers  │         │ (GlobalExceptionHandler) │ │
│  └──────────────┘         └──────────────────────────┘ │
└────────────────────┬────────────────────────────────────┘
                     │ DTOs
┌────────────────────┴────────────────────────────────────┐
│                   Service Layer                          │
│  ┌──────────────────┐    ┌──────────────────────────┐  │
│  │ OrderService     │    │ External Service Clients │  │
│  │ (Business Logic) │◄───│ (Member/Product/Payment) │  │
│  └──────────────────┘    └──────────────────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │ Entities
┌────────────────────┴────────────────────────────────────┐
│                   Data Access Layer                      │
│  ┌──────────────────┐                                   │
│  │ JPA Repositories │                                   │
│  └──────────────────┘                                   │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                   Database Layer                         │
│  ┌──────────────────┐                                   │
│  │  PostgreSQL DB   │                                   │
│  └──────────────────┘                                   │
└─────────────────────────────────────────────────────────┘
```

**Layer Responsibilities:**

1. **Presentation Layer**
   - Handle HTTP requests/responses
   - Request validation using Bean Validation
   - DTO transformation
   - Global exception handling

2. **Service Layer**
   - Core business logic
   - Order validation workflow orchestration
   - External service integration
   - Transaction management

3. **Data Access Layer**
   - Database operations via JPA
   - Query methods
   - Entity management

4. **Database Layer**
   - Data persistence
   - Referential integrity
   - Transaction support

### 2.2 Package Structure

```
com.sotatek.order/
│
├── OrderApplication.java                    # Spring Boot entry point
│
├── controller/                              # Presentation Layer
│   ├── OrderController.java                # REST endpoints
│   └── dto/                                 # Data Transfer Objects
│       ├── CreateOrderRequest.java         # Request DTO for order creation
│       ├── UpdateOrderRequest.java         # Request DTO for order updates
│       ├── OrderResponse.java              # Response DTO for order data
│       ├── OrderItemRequest.java           # Request DTO for order items
│       ├── OrderItemResponse.java          # Response DTO for order items
│       ├── PageResponse.java               # Generic pagination response
│       └── ErrorResponse.java              # Error response format
│
├── service/                                 # Service Layer
│   ├── OrderService.java                   # Business logic interface
│   ├── impl/
│   │   └── OrderServiceImpl.java           # Core order orchestration
│   └── external/                           # External service integration
│       ├── MemberServiceClient.java        # Member API client
│       ├── ProductServiceClient.java       # Product API client
│       ├── PaymentServiceClient.java       # Payment API client
│       └── dto/                            # External service DTOs
│           ├── MemberDto.java
│           ├── ProductDto.java
│           ├── ProductStockDto.java
│           ├── PaymentRequestDto.java
│           └── PaymentDto.java
│
├── repository/                              # Data Access Layer
│   ├── OrderRepository.java                # Order data access
│   └── OrderItemRepository.java            # Order item data access
│
├── domain/                                  # Domain Models
│   ├── Order.java                          # Order entity (JPA)
│   ├── OrderItem.java                      # Order item entity (JPA)
│   ├── OrderStatus.java                    # Order status enum
│   └── PaymentMethod.java                  # Payment method enum
│
├── exception/                               # Exception Handling
│   ├── OrderNotFoundException.java
│   ├── MemberValidationException.java
│   ├── ProductValidationException.java
│   ├── InsufficientStockException.java
│   ├── PaymentFailedException.java
│   ├── InvalidOrderStatusException.java
│   ├── ExternalServiceException.java
│   └── GlobalExceptionHandler.java         # Centralized error handling
│
└── config/                                  # Configuration
    ├── RestTemplateConfig.java             # HTTP client configuration
    ├── ResilienceConfig.java               # Circuit Breaker & Retry config
    └── OpenApiConfig.java                  # Swagger/OpenAPI configuration
```

### 2.3 Design Patterns

**Patterns Implemented:**

1. **Layered Architecture Pattern**
   - Clear separation of concerns
   - Each layer has specific responsibilities
   - Dependencies flow downward

2. **Repository Pattern**
   - Abstracts data access logic
   - Provides collection-like interface for domain objects

3. **Service Layer Pattern**
   - Encapsulates business logic
   - Orchestrates operations across multiple repositories and external services

4. **DTO Pattern**
   - Separates internal domain models from external API contracts
   - Prevents over-exposure of internal data structures

5. **Circuit Breaker Pattern**
   - Prevents cascading failures from external service failures
   - Provides fallback mechanisms

6. **Retry Pattern**
   - Handles transient failures in external service calls
   - Exponential backoff to prevent overwhelming failing services

---

## 3. Technology Stack

### 3.1 Core Technologies

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Language | Java | 17 | Programming language |
| Framework | Spring Boot | 3.2.0 | Application framework |
| Build Tool | Gradle | 8.12 | Build automation |
| Database | PostgreSQL | 16 | Persistent data storage |
| ORM | Hibernate/JPA | 6.3.x | Object-relational mapping |

### 3.2 Key Dependencies

```gradle
dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-aop'

    // Database
    runtimeOnly 'org.postgresql:postgresql'
    runtimeOnly 'com.h2database:h2'  // For testing

    // API Documentation
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'

    // Resilience
    implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.1.0'
    implementation 'io.github.resilience4j:resilience4j-circuitbreaker:2.1.0'
    implementation 'io.github.resilience4j:resilience4j-retry:2.1.0'

    // Utilities
    compileOnly 'org.projectlombok:lombok:1.18.30'
    annotationProcessor 'org.projectlombok:lombok:1.18.30'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.wiremock:wiremock-standalone:3.3.1'
    testImplementation 'com.h2database:h2'
}
```

### 3.3 Infrastructure

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Container Runtime | Docker | Application containerization |
| Container Orchestration | Docker Compose | Multi-container setup |
| HTTP Client | RestTemplate | Synchronous HTTP communication |
| Logging | SLF4J + Logback | Application logging |
| API Documentation | SpringDoc OpenAPI | Interactive API documentation |

---

## 4. Data Model

### 4.1 Entity Relationship Diagram

```
┌──────────────────────────────────────┐
│             orders                    │
├──────────────────────────────────────┤
│ id (PK)              BIGINT          │
│ member_id            BIGINT          │
│ member_name          VARCHAR(255)    │
│ status               VARCHAR(50)     │◄──┐
│ total_amount         DECIMAL(10,2)   │   │
│ payment_method       VARCHAR(50)     │   │
│ payment_id           BIGINT          │   │ One-to-Many
│ transaction_id       VARCHAR(255)    │   │
│ created_at           TIMESTAMP       │   │
│ updated_at           TIMESTAMP       │   │
└──────────────────────────────────────┘   │
                                            │
                                            │
┌──────────────────────────────────────┐   │
│          order_items                  │   │
├──────────────────────────────────────┤   │
│ id (PK)              BIGINT          │   │
│ order_id (FK)        BIGINT          │───┘
│ product_id           BIGINT          │
│ product_name         VARCHAR(255)    │
│ unit_price           DECIMAL(10,2)   │
│ quantity             INTEGER         │
│ subtotal             DECIMAL(10,2)   │
└──────────────────────────────────────┘
```

### 4.2 Order Entity

**Table:** `orders`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique order identifier |
| member_id | BIGINT | NOT NULL | Reference to member in Member Service |
| member_name | VARCHAR(255) | NOT NULL | Cached member name for denormalization |
| status | VARCHAR(50) | NOT NULL | Current order status (enum) |
| total_amount | DECIMAL(10,2) | NOT NULL | Total order amount |
| payment_method | VARCHAR(50) | NULL | Payment method used (enum) |
| payment_id | BIGINT | NULL | Payment ID from Payment Service |
| transaction_id | VARCHAR(255) | NULL | External transaction ID from payment processor |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Order creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Last update timestamp |

**Indexes:**
- Primary key on `id`
- Index on `member_id` for filtering by member
- Index on `status` for filtering by status
- Index on `created_at` for sorting

### 4.3 OrderItem Entity

**Table:** `order_items`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique item identifier |
| order_id | BIGINT | NOT NULL, FOREIGN KEY → orders(id) | Reference to parent order |
| product_id | BIGINT | NOT NULL | Reference to product in Product Service |
| product_name | VARCHAR(255) | NOT NULL | Cached product name |
| unit_price | DECIMAL(10,2) | NOT NULL | Price per unit at time of order |
| quantity | INTEGER | NOT NULL, CHECK > 0 | Quantity ordered |
| subtotal | DECIMAL(10,2) | NOT NULL | Line item subtotal (unit_price * quantity) |

**Indexes:**
- Primary key on `id`
- Index on `order_id` for join optimization
- Index on `product_id` for product-based queries

### 4.4 Enums

**OrderStatus Enum:**

```java
public enum OrderStatus {
    PENDING,         // Order created, validation in progress
    CONFIRMED,       // Validation passed, ready for payment
    PAID,            // Payment successful
    COMPLETED,       // Order fulfilled (future state)
    CANCELLED,       // Order cancelled by user
    PAYMENT_FAILED   // Payment processing failed
}
```

**State Transition Rules:**
```
PENDING ──────→ CONFIRMED ──────→ PAID ──────→ COMPLETED
   │                │
   └→ CANCELLED     └→ PAYMENT_FAILED
```

**PaymentMethod Enum:**

```java
public enum PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    BANK_TRANSFER
}
```

### 4.5 Domain Model Classes

**Order.java:**
```java
@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String memberName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private Long paymentId;

    private String transactionId;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business methods
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean canBeUpdated() {
        return status == OrderStatus.PENDING;
    }

    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }
}
```

---

## 5. API Specification

### 5.1 Base URL

**Local Development:** `http://localhost:8080`
**Docker:** `http://localhost:8080`
**Production:** `https://api.example.com/order-service` (future)

### 5.2 API Endpoints

#### 5.2.1 Create Order

**Endpoint:** `POST /api/orders`

**Description:** Creates a new order with validation through external services and payment processing.

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "memberId": 1001,
  "items": [
    {
      "productId": 2001,
      "quantity": 2
    },
    {
      "productId": 2002,
      "quantity": 1
    }
  ],
  "paymentMethod": "CREDIT_CARD"
}
```

**Request Validation:**
- `memberId`: required, positive integer
- `items`: required, min size 1, max size 100
- `items[].productId`: required, positive integer
- `items[].quantity`: required, min 1, max 10000
- `paymentMethod`: required, enum {CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER}

**Success Response:**
```
HTTP/1.1 201 Created
Content-Type: application/json
Location: /api/orders/1

{
  "id": 1,
  "memberId": 1001,
  "memberName": "John Doe",
  "status": "PAID",
  "items": [
    {
      "id": 1,
      "productId": 2001,
      "productName": "Wireless Mouse",
      "unitPrice": 29.99,
      "quantity": 2,
      "subtotal": 59.98
    },
    {
      "id": 2,
      "productId": 2002,
      "productName": "USB Cable",
      "unitPrice": 9.99,
      "quantity": 1,
      "subtotal": 9.99
    }
  ],
  "totalAmount": 69.97,
  "paymentMethod": "CREDIT_CARD",
  "paymentId": 4001,
  "transactionId": "TXN-20260112-ABC123",
  "createdAt": "2026-01-12T14:30:00Z",
  "updatedAt": "2026-01-12T14:30:05Z"
}
```

**Error Responses:**

```json
// 400 Bad Request - Validation Error
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "timestamp": "2026-01-12T14:30:00Z",
  "errors": [
    {
      "field": "memberId",
      "message": "Member ID is required"
    }
  ]
}

// 400 Bad Request - Member Not Active
{
  "code": "MEMBER_VALIDATION_ERROR",
  "message": "Member is not active: memberId=1001, status=INACTIVE",
  "timestamp": "2026-01-12T14:30:00Z"
}

// 400 Bad Request - Product Not Available
{
  "code": "PRODUCT_VALIDATION_ERROR",
  "message": "Product is not available: productId=2001, status=OUT_OF_STOCK",
  "timestamp": "2026-01-12T14:30:00Z"
}

// 400 Bad Request - Insufficient Stock
{
  "code": "INSUFFICIENT_STOCK",
  "message": "Insufficient stock for product: productId=2001, requested=5, available=2",
  "timestamp": "2026-01-12T14:30:00Z"
}

// 404 Not Found - Member Not Found
{
  "code": "MEMBER_NOT_FOUND",
  "message": "Member not found: memberId=1001",
  "timestamp": "2026-01-12T14:30:00Z"
}

// 422 Unprocessable Entity - Payment Failed
{
  "code": "PAYMENT_FAILED",
  "message": "Payment processing failed: Insufficient funds",
  "timestamp": "2026-01-12T14:30:00Z"
}

// 503 Service Unavailable - External Service Down
{
  "code": "EXTERNAL_SERVICE_UNAVAILABLE",
  "message": "Member service is currently unavailable. Please try again later.",
  "timestamp": "2026-01-12T14:30:00Z"
}
```

#### 5.2.2 Get Order by ID

**Endpoint:** `GET /api/orders/{id}`

**Description:** Retrieves a specific order by its ID.

**Path Parameters:**
- `id` (required): Order ID (integer)

**Success Response:**
```
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 1,
  "memberId": 1001,
  "memberName": "John Doe",
  "status": "PAID",
  "items": [...],
  "totalAmount": 69.97,
  "paymentMethod": "CREDIT_CARD",
  "paymentId": 4001,
  "transactionId": "TXN-20260112-ABC123",
  "createdAt": "2026-01-12T14:30:00Z",
  "updatedAt": "2026-01-12T14:30:05Z"
}
```

**Error Response:**
```json
// 404 Not Found
{
  "code": "ORDER_NOT_FOUND",
  "message": "Order not found: id=1",
  "timestamp": "2026-01-12T14:30:00Z"
}
```

#### 5.2.3 List Orders (Paginated)

**Endpoint:** `GET /api/orders`

**Description:** Retrieves a paginated list of orders with optional filtering.

**Query Parameters:**
- `page` (optional): Page number, 0-indexed (default: 0)
- `size` (optional): Page size (default: 10, max: 100)
- `memberId` (optional): Filter by member ID
- `status` (optional): Filter by status {PENDING, CONFIRMED, PAID, COMPLETED, CANCELLED, PAYMENT_FAILED}
- `sort` (optional): Sort field and direction, e.g., "createdAt,desc" (default: "createdAt,desc")

**Example Request:**
```
GET /api/orders?page=0&size=10&memberId=1001&status=PAID&sort=createdAt,desc
```

**Success Response:**
```
HTTP/1.1 200 OK
Content-Type: application/json

{
  "content": [
    {
      "id": 1,
      "memberId": 1001,
      "memberName": "John Doe",
      "status": "PAID",
      "totalAmount": 69.97,
      "paymentMethod": "CREDIT_CARD",
      "createdAt": "2026-01-12T14:30:00Z",
      "updatedAt": "2026-01-12T14:30:05Z"
    },
    {
      "id": 2,
      "memberId": 1001,
      "memberName": "John Doe",
      "status": "PAID",
      "totalAmount": 149.99,
      "paymentMethod": "DEBIT_CARD",
      "createdAt": "2026-01-11T10:15:00Z",
      "updatedAt": "2026-01-11T10:15:10Z"
    }
  ],
  "page": {
    "number": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3
  }
}
```

#### 5.2.4 Update Order

**Endpoint:** `PUT /api/orders/{id}`

**Description:** Updates an existing order. Only PENDING orders can be updated.

**Path Parameters:**
- `id` (required): Order ID (integer)

**Request Body:**
```json
{
  "items": [
    {
      "productId": 2001,
      "quantity": 3
    }
  ],
  "paymentMethod": "DEBIT_CARD"
}
```

**Success Response:**
```
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 1,
  "memberId": 1001,
  "memberName": "John Doe",
  "status": "PENDING",
  "items": [
    {
      "id": 3,
      "productId": 2001,
      "productName": "Wireless Mouse",
      "unitPrice": 29.99,
      "quantity": 3,
      "subtotal": 89.97
    }
  ],
  "totalAmount": 89.97,
  "paymentMethod": "DEBIT_CARD",
  "createdAt": "2026-01-12T14:30:00Z",
  "updatedAt": "2026-01-12T15:45:00Z"
}
```

**Error Responses:**
```json
// 400 Bad Request - Invalid Status
{
  "code": "INVALID_ORDER_STATUS",
  "message": "Cannot update order with status: PAID. Only PENDING orders can be updated.",
  "timestamp": "2026-01-12T15:45:00Z"
}

// 404 Not Found
{
  "code": "ORDER_NOT_FOUND",
  "message": "Order not found: id=1",
  "timestamp": "2026-01-12T15:45:00Z"
}
```

#### 5.2.5 Cancel Order

**Endpoint:** `DELETE /api/orders/{id}`

**Description:** Cancels an order by changing its status to CANCELLED. Only PENDING or CONFIRMED orders can be cancelled.

**Path Parameters:**
- `id` (required): Order ID (integer)

**Success Response:**
```
HTTP/1.1 204 No Content
```

**Error Responses:**
```json
// 400 Bad Request - Invalid Status
{
  "code": "INVALID_ORDER_STATUS",
  "message": "Cannot cancel order with status: PAID. Only PENDING or CONFIRMED orders can be cancelled.",
  "timestamp": "2026-01-12T16:00:00Z"
}

// 404 Not Found
{
  "code": "ORDER_NOT_FOUND",
  "message": "Order not found: id=1",
  "timestamp": "2026-01-12T16:00:00Z"
}
```

### 5.3 HTTP Status Codes

| Status Code | Meaning | Usage |
|-------------|---------|-------|
| 200 OK | Success | GET, PUT operations successful |
| 201 Created | Resource created | POST order created successfully |
| 204 No Content | Success with no body | DELETE order cancelled |
| 400 Bad Request | Client error | Validation error, business rule violation |
| 404 Not Found | Resource not found | Order, Member, or Product not found |
| 422 Unprocessable Entity | Processing error | Payment failed |
| 500 Internal Server Error | Server error | Unexpected server error |
| 503 Service Unavailable | Service down | External service unavailable |

---

## 6. External Service Integration

### 6.1 Member Service

**Base URL:** `http://member-service:8081` (configurable)

**Endpoint:** `GET /api/members/{memberId}`

**Purpose:** Validate member exists and is active before creating orders.

**Integration Point:** Order creation and update validation

**Request Example:**
```
GET http://member-service:8081/api/members/1001
```

**Response Example:**
```json
{
  "id": 1001,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "status": "ACTIVE",
  "grade": "GOLD"
}
```

**Error Handling:**
- 404 Not Found → Throw `MemberNotFoundException`
- Member status != ACTIVE → Throw `MemberValidationException`
- Timeout/Connection Error → Throw `ExternalServiceException`

**Circuit Breaker Configuration:**
- Instance: `memberService`
- Failure threshold: 50%
- Wait duration: 10s
- Sliding window: 10 calls

**Retry Configuration:**
- Max attempts: 3
- Wait duration: 1s
- Backoff multiplier: 2

### 6.2 Product Service

**Base URL:** `http://product-service:8082` (configurable)

**Endpoints:**

1. **Get Product Details**
   ```
   GET /api/products/{productId}
   ```
   Response:
   ```json
   {
     "id": 2001,
     "name": "Wireless Mouse",
     "price": 29.99,
     "status": "AVAILABLE"
   }
   ```

2. **Get Product Stock**
   ```
   GET /api/products/{productId}/stock
   ```
   Response:
   ```json
   {
     "productId": 2001,
     "quantity": 150,
     "reservedQuantity": 10,
     "availableQuantity": 140
   }
   ```

**Integration Point:** Order creation and update validation

**Validation Rules:**
- Product must exist (404 → `ProductValidationException`)
- Product status must be "AVAILABLE"
- availableQuantity must be >= requested quantity → `InsufficientStockException`

**Circuit Breaker Configuration:**
- Instance: `productService`
- Failure threshold: 50%
- Wait duration: 10s

### 6.3 Payment Service

**Base URL:** `http://payment-service:8083` (configurable)

**Endpoint:** `POST /api/payments`

**Purpose:** Process payment for confirmed orders.

**Integration Point:** After order validation succeeds, before final status update

**Request Example:**
```json
{
  "orderId": 1,
  "amount": 69.97,
  "paymentMethod": "CREDIT_CARD"
}
```

**Response Example:**
```json
{
  "id": 4001,
  "orderId": 1,
  "amount": 69.97,
  "status": "COMPLETED",
  "transactionId": "TXN-20260112-ABC123",
  "createdAt": "2026-01-12T14:30:05Z"
}
```

**Status Handling:**
- `COMPLETED` → Update order status to PAID, save payment details
- `FAILED` → Update order status to PAYMENT_FAILED
- `PENDING` → Keep order in CONFIRMED state (edge case)

**Error Handling:**
- 400 Bad Request → `PaymentFailedException`
- 422 Unprocessable Entity → `PaymentFailedException` (insufficient funds, etc.)
- Timeout/Connection Error → `ExternalServiceException`

**Circuit Breaker Configuration:**
- Instance: `paymentService`
- Failure threshold: 50%
- Wait duration: 10s

### 6.4 Resilience Patterns

**Circuit Breaker Strategy:**

```
┌─────────────────────────────────────────────────────┐
│              Circuit Breaker States                  │
│                                                      │
│   CLOSED ──failure──> OPEN ──timeout──> HALF_OPEN  │
│     │        rate                           │        │
│     │       > 50%                            │        │
│     │                                        │        │
│     └───────────────success─────────────────┘        │
│                                                      │
│   CLOSED:     Normal operation, tracking failures   │
│   OPEN:       Failing fast, rejecting requests      │
│   HALF_OPEN:  Testing if service recovered          │
└─────────────────────────────────────────────────────┘
```

**Retry Strategy:**

```
Attempt 1 ─failure─> Wait 1s ─> Attempt 2 ─failure─> Wait 2s ─> Attempt 3
    │                                │                                │
  Success                          Success                       Success/Failure
```

**Fallback Strategy:**
- Return user-friendly error messages
- Log detailed error for debugging
- Preserve order data for manual review

---

## 7. Error Handling Strategy

### 7.1 Exception Hierarchy

```
RuntimeException
    │
    ├─ OrderException (base)
    │   ├─ OrderNotFoundException
    │   ├─ InvalidOrderStatusException
    │   ├─ MemberValidationException
    │   ├─ ProductValidationException
    │   ├─ InsufficientStockException
    │   ├─ PaymentFailedException
    │   └─ ExternalServiceException
    │
    └─ MethodArgumentNotValidException (Spring)
```

### 7.2 Exception to HTTP Status Mapping

| Exception | HTTP Status | Error Code |
|-----------|-------------|------------|
| OrderNotFoundException | 404 Not Found | ORDER_NOT_FOUND |
| MemberValidationException | 400 Bad Request | MEMBER_VALIDATION_ERROR |
| ProductValidationException | 400 Bad Request | PRODUCT_VALIDATION_ERROR |
| InsufficientStockException | 400 Bad Request | INSUFFICIENT_STOCK |
| InvalidOrderStatusException | 400 Bad Request | INVALID_ORDER_STATUS |
| PaymentFailedException | 422 Unprocessable Entity | PAYMENT_FAILED |
| ExternalServiceException | 503 Service Unavailable | EXTERNAL_SERVICE_UNAVAILABLE |
| MethodArgumentNotValidException | 400 Bad Request | VALIDATION_ERROR |
| Generic Exception | 500 Internal Server Error | INTERNAL_SERVER_ERROR |

### 7.3 Error Response Format

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable error message",
  "timestamp": "2026-01-12T14:30:00Z",
  "errors": [                              // Optional, for validation errors
    {
      "field": "fieldName",
      "message": "Field-specific error message"
    }
  ]
}
```

### 7.4 Global Exception Handler

**Implementation:** `@RestControllerAdvice` annotated class

**Responsibilities:**
1. Catch all exceptions from controllers
2. Log exceptions with appropriate level
3. Transform exceptions to standardized error responses
4. Set appropriate HTTP status codes
5. Sanitize error messages (don't expose stack traces to clients)

---

## 8. Security Considerations

### 8.1 Input Validation

**Validation Rules:**
- All request DTOs use Bean Validation annotations (@NotNull, @Min, @Max, @Size, etc.)
- Custom validators for business rules
- Sanitize inputs to prevent injection attacks

**Example:**
```java
@NotNull(message = "Member ID is required")
@Positive(message = "Member ID must be positive")
private Long memberId;

@NotEmpty(message = "Order must contain at least one item")
@Size(max = 100, message = "Order cannot contain more than 100 items")
private List<OrderItemRequest> items;
```

### 8.2 SQL Injection Prevention

- Use JPA/Hibernate for database access (parameterized queries)
- Never concatenate user input into queries
- Use typed query parameters

### 8.3 Sensitive Data

**Handling:**
- Do not log payment details (credit card numbers, CVV)
- Store only payment IDs and transaction references
- Mask sensitive data in logs

**Example:**
```java
log.info("Processing payment for order: {}, amount: {}",
         orderId,
         amount); // ✅ OK

log.info("Processing payment: {}", paymentRequest); // ❌ May expose sensitive data
```

### 8.4 External Service Communication

**Security Measures:**
- Use HTTPS for production external service calls
- Implement request timeouts to prevent DoS
- Validate SSL certificates
- Use API keys/tokens for authentication (future enhancement)

### 8.5 Rate Limiting

**Recommendation:** Implement at API Gateway level (out of scope for this service)

**Application-Level Fallback:**
- Circuit Breaker prevents overwhelming external services
- Database connection pooling prevents resource exhaustion

---

## 9. Testing Strategy

### 9.1 Test Pyramid

```
                    /\
                   /  \
                  / E2E \               10% - Integration Tests
                 /──────\
                /        \
               / Service  \             30% - Service Layer Tests
              /   Layer    \
             /──────────────\
            /                \
           /   Unit Tests     \        60% - Unit Tests
          /____________________\
```

### 9.2 Unit Tests

**Target Coverage:** >80%

**Service Layer Tests:**
- Test file: `OrderServiceImplTest.java`
- Mock dependencies: OrderRepository, External Service Clients
- Test scenarios:
  - ✅ Create order success
  - ✅ Update order success
  - ✅ Cancel order success
  - ❌ Member not found
  - ❌ Member not active
  - ❌ Product not found
  - ❌ Product not available
  - ❌ Insufficient stock
  - ❌ Payment failed
  - ❌ Update non-pending order
  - ❌ Cancel paid order

**Controller Layer Tests:**
- Test file: `OrderControllerTest.java`
- Use `@WebMvcTest` and MockMvc
- Mock service layer
- Test scenarios:
  - Request/response JSON serialization
  - HTTP status codes
  - Validation error handling
  - Exception handler integration

**Example Test:**
```java
@Test
void createOrder_whenMemberNotActive_shouldThrowException() {
    // Given
    CreateOrderRequest request = createValidOrderRequest();
    MemberDto inactiveMember = createMemberDto(INACTIVE);
    when(memberServiceClient.getMember(anyLong()))
        .thenReturn(inactiveMember);

    // When & Then
    assertThatThrownBy(() -> orderService.createOrder(request))
        .isInstanceOf(MemberValidationException.class)
        .hasMessageContaining("Member is not active");
}
```

### 9.3 Integration Tests

**Target:** End-to-end order flow

**Test file:** `OrderIntegrationTest.java`

**Setup:**
- `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- Use `TestRestTemplate` for API calls
- Use WireMock for external service mocking
- Use H2 in-memory database

**Test scenarios:**
1. Complete order flow: Create → Get → List → Update → Cancel
2. Order creation with payment success
3. Order creation with payment failure
4. External service unavailability handling
5. Pagination and filtering

**Example Test:**
```java
@Test
void createOrder_endToEndFlow_shouldCreateOrderAndProcessPayment() {
    // Given: Mock external services with WireMock
    stubMemberService(ACTIVE_MEMBER_RESPONSE);
    stubProductService(AVAILABLE_PRODUCT_RESPONSE);
    stubPaymentService(SUCCESSFUL_PAYMENT_RESPONSE);

    CreateOrderRequest request = createValidOrderRequest();

    // When: Create order via REST API
    ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
        "/api/orders",
        request,
        OrderResponse.class
    );

    // Then: Verify response
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    OrderResponse order = response.getBody();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

    // And: Verify database state
    Order savedOrder = orderRepository.findById(order.getId()).get();
    assertThat(savedOrder.getPaymentId()).isNotNull();
}
```

### 9.4 Test Data Management

**Strategy:**
- Create test data builders for clean test setup
- Use constants for common test values
- Avoid hardcoding test data in multiple places

**Example:**
```java
public class TestDataBuilder {
    public static CreateOrderRequest createValidOrderRequest() {
        return CreateOrderRequest.builder()
            .memberId(1001L)
            .items(List.of(
                OrderItemRequest.builder()
                    .productId(2001L)
                    .quantity(2)
                    .build()
            ))
            .paymentMethod(PaymentMethod.CREDIT_CARD)
            .build();
    }

    public static MemberDto createActiveMember() {
        return MemberDto.builder()
            .id(1001L)
            .name("John Doe")
            .status("ACTIVE")
            .build();
    }
}
```

### 9.5 Mocking External Services

**Unit Tests:** Use Mockito
```java
@Mock
private MemberServiceClient memberServiceClient;

@BeforeEach
void setUp() {
    when(memberServiceClient.getMember(1001L))
        .thenReturn(createActiveMember());
}
```

**Integration Tests:** Use WireMock
```java
@Test
void testWithWireMock() {
    stubFor(get(urlEqualTo("/api/members/1001"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(activeMemberJson)));
}
```

---

## 10. Deployment Guide

### 10.1 Prerequisites

- Docker 20+ and Docker Compose 2+
- Java 17 JDK (for local development)
- Gradle 8.12 (or use included wrapper)
- PostgreSQL 16 (via Docker)

### 10.2 Local Development Setup

**1. Start PostgreSQL:**
```bash
docker-compose up postgres -d
```

**2. Run Application:**
```bash
./gradlew bootRun
```

**3. Access Services:**
- Application: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console (if enabled): http://localhost:8080/h2-console

### 10.3 Docker Deployment

**Build Docker Image:**
```bash
docker-compose build
```

**Start Full Stack:**
```bash
docker-compose up -d
```

**Stop Services:**
```bash
docker-compose down
```

**View Logs:**
```bash
docker-compose logs -f order-service
```

### 10.4 Configuration Management

**Environment Variables:**

| Variable | Description | Default |
|----------|-------------|---------|
| SPRING_DATASOURCE_URL | PostgreSQL JDBC URL | jdbc:postgresql://localhost:5432/orderdb |
| SPRING_DATASOURCE_USERNAME | Database username | orderuser |
| SPRING_DATASOURCE_PASSWORD | Database password | orderpass |
| MEMBER_SERVICE_URL | Member Service base URL | http://localhost:8081 |
| PRODUCT_SERVICE_URL | Product Service base URL | http://localhost:8082 |
| PAYMENT_SERVICE_URL | Payment Service base URL | http://localhost:8083 |
| SERVER_PORT | Application port | 8080 |

**Profile-Specific Configuration:**

- `application.yml`: Default configuration
- `application-dev.yml`: Development profile
- `application-test.yml`: Testing profile (H2 database)
- `application-prod.yml`: Production profile (future)

**Activate Profile:**
```bash
# Via command line
./gradlew bootRun --args='--spring.profiles.active=dev'

# Via environment variable
export SPRING_PROFILES_ACTIVE=dev
./gradlew bootRun
```

### 10.5 Health Checks

**Actuator Endpoints:**

- `/actuator/health` - Application health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics

**Docker Health Check:**
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

---

## 11. Monitoring and Logging

### 11.1 Logging Strategy

**Log Levels:**
- **ERROR**: Critical errors requiring immediate attention
- **WARN**: Potential issues, degraded functionality
- **INFO**: Business operations, key milestones
- **DEBUG**: Detailed execution flow (development only)

**Logging Points:**

1. **Controller Layer:**
   ```java
   log.info("Received create order request: memberId={}", request.getMemberId());
   log.info("Order created successfully: orderId={}", order.getId());
   ```

2. **Service Layer:**
   ```java
   log.debug("Validating member: memberId={}", memberId);
   log.info("Order validation completed: orderId={}", order.getId());
   log.error("Payment failed for order: orderId={}, error={}", orderId, error);
   ```

3. **External Service Calls:**
   ```java
   log.debug("Calling Member Service: memberId={}", memberId);
   log.warn("Member Service call failed, retrying: attempt={}", attemptNumber);
   log.error("Member Service unavailable after retries: memberId={}", memberId);
   ```

**Log Format:**
```
timestamp level [thread] logger : message
2026-01-12T14:30:00.123Z INFO [http-nio-8080-exec-1] c.s.o.controller.OrderController : Order created successfully: orderId=1
```

### 11.2 Structured Logging

**Recommendation:** Use structured logging for production

**Example with MDC (Mapped Diagnostic Context):**
```java
MDC.put("orderId", order.getId().toString());
MDC.put("memberId", order.getMemberId().toString());
log.info("Processing order");
MDC.clear();
```

### 11.3 Metrics

**Key Metrics to Track:**

1. **Business Metrics:**
   - Orders created per hour
   - Order success rate
   - Payment success rate
   - Average order value

2. **Technical Metrics:**
   - Request latency (p50, p95, p99)
   - Error rate per endpoint
   - External service call latency
   - Circuit breaker state changes
   - Database connection pool usage

**Implementation:**
- Use Spring Boot Actuator metrics
- Integrate with Micrometer (built-in)
- Export to monitoring system (Prometheus, Grafana - future)

### 11.4 Alerting

**Critical Alerts:**
- Error rate > 5%
- External service circuit breaker open
- Database connection pool exhausted
- Payment failure rate > 10%

---

## 12. Development Guidelines

### 12.1 Code Style

**Java Conventions:**
- Follow standard Java naming conventions
- Use meaningful variable and method names
- Keep methods small and focused (single responsibility)
- Maximum method length: 30 lines
- Maximum class length: 300 lines

**Spring Boot Conventions:**
- Use constructor injection over field injection
- Prefer `@RestController` over `@Controller` + `@ResponseBody`
- Use `@Slf4j` Lombok annotation for logging

### 12.2 Git Workflow

**Branch Strategy:**
- `main`: Production-ready code
- `develop`: Integration branch
- `feature/*`: Feature branches
- `bugfix/*`: Bug fix branches

**Commit Messages:**
```
type: subject

body (optional)

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```

**Types:** feat, fix, docs, test, refactor, chore

### 12.3 Code Review Checklist

- [ ] Code follows style guidelines
- [ ] All tests pass
- [ ] Test coverage maintained (>80%)
- [ ] No hardcoded values (use configuration)
- [ ] Proper error handling
- [ ] Logging added at appropriate points
- [ ] Documentation updated
- [ ] No security vulnerabilities

### 12.4 Performance Considerations

**Database:**
- Use appropriate indexes
- Avoid N+1 query problems (use JOIN FETCH)
- Use pagination for list queries

**External Service Calls:**
- Always use timeouts
- Implement circuit breakers
- Consider caching frequently accessed data (future)

**Memory:**
- Close resources properly (use try-with-resources)
- Avoid loading large datasets into memory
- Use streaming for large responses

---

## Appendix

### A. Glossary

| Term | Definition |
|------|------------|
| Circuit Breaker | Design pattern that prevents cascading failures by failing fast when a service is unavailable |
| DTO | Data Transfer Object - Object used to transfer data between layers |
| JPA | Java Persistence API - Java specification for ORM |
| Retry Pattern | Automatically retry failed operations with exponential backoff |
| Resilience | Ability of a system to handle and recover from failures |

### B. References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [REST API Best Practices](https://restfulapi.net/)

### C. Contact Information

**Development Team:** Backend Engineering
**Assessment Contact:** Your Interviewer
**Documentation Version:** 1.0.0
**Last Updated:** January 12, 2026

---

**End of Technical Documentation**
