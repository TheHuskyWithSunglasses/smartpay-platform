# smartpay-# SmartPay API Platform

![CI](https://github.com/TheHuskyWithSunglasses/smartpay-platform/actions/workflows/ci.yml/badge.svg)

A production-grade **B2B payment management platform** built as a portfolio project to demonstrate mid-senior Java backend engineering skills. SmartPay exposes a REST API that allows merchants to process payments, receive webhook notifications on payment state changes, and query financial statistics.

---

## Architecture

SmartPay is a multi-module Maven microservices platform composed of four independently deployable services:

```
                        ┌─────────────────┐
                        │   API Gateway   │  :8080
                        │  JWT Validation │
                        │  Rate Limiting  │
                        └────────┬────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                                     │
   ┌──────────▼──────────┐             ┌────────────▼────────┐
   │  merchant-service   │             │  payment-service    │
   │  :8083              │             │  :8081              │
   │  Registration       │             │  Payment CRUD       │
   │  Authentication     │             │  State Machine      │
   │  JWT / Refresh      │             │  Idempotency        │
   │  Tokens             │             │  Stats              │
   └─────────────────────┘             └────────────┬────────┘
                                                    │
                                              Kafka Event
                                                    │
                                       ┌────────────▼────────┐
                                       │ notification-service │
                                       │ :8082               │
                                       │ Webhook Delivery    │
                                       │ Retry / Backoff     │
                                       │ Dead Letter         │
                                       └─────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| API Gateway | Spring Cloud Gateway (WebFlux) |
| Database | PostgreSQL 15 |
| Cache / Rate Limiting | Redis 7 |
| Messaging | Apache Kafka 3.7 |
| Migrations | Liquibase |
| Security | Spring Security, JJWT 0.13 |
| Build | Maven (multi-module) |
| Containerisation | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| Testing | JUnit 5, Mockito |

---

## Key Technical Features

### API Gateway
- JWT validation on every protected route — extracts `merchantId` from token claims and forwards as `X-Merchant-Id` header
- Redis-backed rate limiting using the **Token Bucket algorithm** (10 req/s, burst 20)
- Public routes (`/auth/register`, `/auth/login`) bypass authentication

### Authentication
- Dual-token flow: short-lived JWT access token + long-lived refresh token
- Refresh tokens stored as **SHA-256 hashes** — raw token never persisted
- **Token rotation** on every refresh — old token revoked, new one issued
- RFC 7807 Problem Details for all error responses

### Payments
- **Payment state machine** enforced at domain level — `setStatus()` validates transitions and throws `InvalidStateTransitionException` on illegal moves
- State flow: `PENDING → PROCESSING → COMPLETED / FAILED → REFUNDED`
- **Idempotency** via unique constraint on `idempotency_key` — duplicate requests return the original response without reprocessing
- Dynamic filtering with **JPA Specifications** — filter by merchant, status, and date range with pagination
- Amount stored as `BIGINT` (cents) — no floating point precision issues

### Webhook Delivery
- Event-driven via **Apache Kafka** — `payment-service` publishes events on terminal state transitions
- `notification-service` consumes events and delivers `POST` webhooks to merchant URLs
- **Exponential backoff retry**: 3 attempts with delays of 1s, 5s, 25s
- Failed deliveries marked `DEAD` and persisted for manual inspection
- **Idempotent delivery** — duplicate Kafka messages detected and skipped

---

## Project Structure

```
smartpay-platform/
├── api-gateway/              # Spring Cloud Gateway — routing, JWT, rate limiting
├── merchant-service/         # Auth, registration, merchant profile
├── payment-service/          # Payment processing, state machine, stats
├── notification-service/     # Kafka consumer, webhook delivery
├── docker-compose.yml        # Full local stack
└── pom.xml                   # Parent POM
```

---

## Quick Start

### Prerequisites
- Docker and Docker Compose
- Java 21
- Maven

### 1. Clone the repository

```bash
git clone https://github.com/TheHuskyWithSunglasses/smartpay-platform.git
cd smartpay-platform
```

### 2. Start infrastructure

```bash
docker-compose up -d
```

This starts:
- PostgreSQL (merchant DB on `:5432`, payment DB on `:5433`, notification DB on `:5434`)
- Redis on `:6379`
- Apache Kafka on `:9092`

### 3. Set environment variables

```bash
export JWT_SECRET_KEY=<your-base64-encoded-secret>
```

Generate a secret:
```bash
openssl rand -base64 32
```

### 4. Start the services

Start each service in order:
```
merchant-service    → port 8083
payment-service     → port 8081
notification-service → port 8082
api-gateway         → port 8080
```

All requests should go through the gateway on **port 8080**.

---

## API Overview

All endpoints below go through the API Gateway on `:8080`.

### Authentication (public)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register a new merchant |
| POST | `/api/v1/auth/login` | Login and receive tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Revoke refresh tokens |

### Merchant Profile (authenticated)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/merchants/me` | Get current merchant profile |
| PUT | `/api/v1/merchants/me` | Update business name |

### Payments (authenticated)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/payments` | Create a payment |
| GET | `/api/v1/payments` | List payments with filters |
| GET | `/api/v1/payments/{id}` | Get payment by ID |
| PATCH | `/api/v1/payments/{id}/status` | Update payment status (processor callback) |
| POST | `/api/v1/payments/{id}/refund` | Refund a completed payment |
| GET | `/api/v1/payments/stats` | Get payment statistics |

### Authentication Header

All protected endpoints require:
```
Authorization: Bearer <access_token>
```

---

## Payment Flow Example

```bash
# 1. Register
POST /api/v1/auth/register
{ "email": "merchant@example.com", "password": "secret", "businessName": "Acme Ltd" }

# 2. Login
POST /api/v1/auth/login
{ "email": "merchant@example.com", "password": "secret" }
# → returns accessToken + refreshToken

# 3. Create payment
POST /api/v1/payments
Authorization: Bearer <accessToken>
{ "amount": 99.99, "currency": "EUR", "idempotencyKey": "order-123", "webhookUrl": "https://yoursite.com/webhook" }

# 4. Simulate processor callback
PATCH /api/v1/payments/{id}/status
{ "status": "PROCESSING" }

PATCH /api/v1/payments/{id}/status
{ "status": "COMPLETED" }
# → Kafka event published → webhook delivered to your URL
```

---

## Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Build tool | Maven multi-module | Explicit dependency management, standard in enterprise Java |
| Migrations | Liquibase | Schema ownership, rollback support, no auto-ddl |
| DTOs | Java records | Immutable, concise, no Lombok needed |
| Entities | Lombok | Reduces boilerplate for mutable domain objects |
| UUIDs | Hibernate-generated | No DB round-trip for ID generation |
| Timestamps | `OffsetDateTime` | Timezone-aware, maps to PostgreSQL `TIMESTAMPTZ` |
| Error responses | RFC 7807 Problem Details | Industry standard, consistent structure |
| Amount storage | `BIGINT` (cents) | Avoids floating-point precision issues |
| Cross-service IDs | UUID reference (no FK) | Services own their own schemas |

---

## Running Tests

```bash
mvn test -pl merchant-service
mvn test -pl payment-service
mvn test -pl notification-service
mvn test -pl api-gateway
```

Or all at once:

```bash
mvn test
```platform