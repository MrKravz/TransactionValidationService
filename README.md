# Transaction Validation Service

A high-performance banking microservice built with **Java 21** and **Spring Boot 4** for real-time expense transaction ingestion, currency conversion, monthly limit tracking, and pessimistic concurrency control.

---

## Key Features

- **Real-Time Transaction Ingestion**: Ingests transactions in local/foreign currencies (`KZT`, `RUB`, `USD`, etc.), converts amounts to USD using daily exchange rates, and stores records in PostgreSQL.
- **Pessimistic Concurrency Control**: Uses PostgreSQL row-level locks (`SELECT ... FOR UPDATE` on `limit_locks`) within a single database transaction to guarantee thread-safe limit evaluations during parallel payments.
- **Dynamic Limit Management**: Tracks expense limits separately for `product` (goods) and `service` (services) categories (default: **$1000.00 USD**).
- **Exchange Rate Integration & Resilience**: Fetches close rates from TwelveData API with fallback to previous closing rates, wrapped in Resilience4j (Circuit Breaker, Bulkhead, Retry) and fast Redis caching.
- **Model Context Protocol (MCP) AI Integration**:
  - **Spring AI MCP Server**: Exposes domain client APIs (`getClientLimits`, `getExceededTransactions`) to AI agents over SSE (`/mcp/sse`).
  - **PostgreSQL Read-Only MCP Agent**: Allows AI agents to read DB schema and execute safe read-only queries.

---

## Tech Stack

| Category | Technology |
| :--- | :--- |
| **Language & Runtime** | Java 21 (Virtual Threads enabled, Records, Pattern Matching) |
| **Framework** | Spring Boot 4, Spring Cloud OpenFeign, Spring AI MCP |
| **Data Access** | jOOQ, Spring Data JPA, Liquibase (YAML Schema-First migrations) |
| **Database & Cache** | PostgreSQL 15, Redis (`StringRedisTemplate`) |
| **Resilience & Fault Tolerance** | Resilience4j (Circuit Breaker, Retry, Bulkhead) |
| **Documentation & Validation** | Springdoc OpenAPI (Swagger UI), Jakarta Bean Validation (RFC 7807 ProblemDetail) |
| **Testing** | JUnit 5, Mockito, Testcontainers (PostgreSQL, Redis) |

---

## Getting Started

### Prerequisites
- **Java 21 JDK**
- **Docker & Docker Compose**

### 1. Environment Configuration
Copy the template environment file:
```bash
cp .env.example .env
```
Edit `.env` to configure your credentials (`DB_PASSWORD`, `API_KEY`, etc.).

### 2. Start Infrastructure Services
Spin up PostgreSQL and Redis containers using Docker Compose:
```bash
docker compose up -d postgres redis
```

### 3. Provision Read-Only DB User (for MCP Agent)
Run the setup script to create a restricted `readonly_user` for database inspection:
```bash
docker exec -i transaction_validation_postgres psql -U postgres -d transaction_validation_service_db -v READONLY_PASS="$DB_READONLY_PASSWORD" < scripts/create_readonly_user.sql
```

### 4. Build and Run the Application
Build the project using Maven Wrapper:
```bash
./mvnw clean compile
```
Run the application locally:
```bash
./mvnw spring-boot:run
```
- **REST Service Port:** `8080`
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Spring AI MCP Endpoint:** [http://localhost:8080/mcp/sse](http://localhost:8080/mcp/sse)

---

## Running Tests

Execute the unit and integration test suite (spins up PostgreSQL & Redis via Testcontainers):
```bash
./mvnw test
```

---

## REST API Endpoint Summary

### Integration API
* **`POST /api/v1/transactions`** — Ingest incoming transactions.
```json
{
  "account_from": "0000000123",
  "account_to": "9999999999",
  "currency_shortname": "RUB",
  "sum": 1500000,
  "expense_category": "product",
  "datetime": "2026-10-06T00:00:00+03:00"
}
```

### Client API
* **`POST /api/v1/client/limits`** — Set a new expense limit.
* **`GET /api/v1/client/limits?account_from=0000000123`** — List all historical limits for a client.
* **`GET /api/v1/client/transactions/exceeded?account_from=0000000123`** — List transactions that exceeded limits.

---

## Model Context Protocol (MCP) Integration

Workspace configuration file is located at [`.agents/mcp_config.json`](file:///c:/Projects/TransactionValidationService/.agents/mcp_config.json):

1. **`postgres-readonly`**: Connects via Stdio using environment variables (`PGUSER`, `PGPASSWORD`) to query database tables safely.
2. **`client-api-mcp`**: Connects via SSE to `http://localhost:8080/mcp/sse` to invoke Spring AI tools (`getClientLimits`, `getExceededTransactions`).

Skill runbook and guide: [`.agents/skills/postgres-client-mcp/SKILL.md`](file:///c:/Projects/TransactionValidationService/.agents/skills/postgres-client-mcp/SKILL.md).

---

## Project Structure

```text
TransactionValidationService/
├── .agents/                    # Workspace agent configurations & MCP setup
│   ├── mcp_config.json         # MCP server definitions
│   └── skills/
│       └── postgres-client-mcp # Runbook skill for MCP integration
├── scripts/
│   └── create_readonly_user.sql # Read-only DB user setup script
├── src/main/java/by/ares/transaction_validation_service/
│   ├── config/                 # Application configuration & Spring AI setup
│   ├── controller/             # REST Controllers & OpenAPI interfaces
│   ├── dto/                    # Data Transfer Objects
│   ├── feign/                  # OpenFeign clients & Fallbacks
│   ├── mcp/                    # Spring AI ClientMcpTools implementations
│   ├── model/                  # JPA & Domain Entities
│   ├── repository/             # JPA & jOOQ Repositories
│   ├── service/                # Business logic & Transaction execution services
│   └── util/                   # Date & Constant utilities
├── src/main/resources/
│   ├── db/changelog/           # Liquibase YAML schema migrations
│   └── application.yaml        # Spring Boot application properties
├── Dockerfile                  # Application container image build definition
├── docker-compose.yaml         # Multi-container orchestration (App, Postgres, Redis)
├── AGENTS.md                   # System guidelines & AI Agent runbook
└── README.md                   # Project documentation
```
