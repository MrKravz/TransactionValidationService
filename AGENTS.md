# AGENTS.md — System & Architectural Guidelines

## System Overview & Objectives
The microservice is designed to:
- Ingest real-time expense transactions in foreign and local currencies (KZT, RUB, etc.) and save them to its dedicated database.
- Convert transaction amounts into USD using daily close rates (`close` or `previous_close`).
- Track and manage monthly expense limits in USD separately for two categories: `product` and `service` (default: 1000.00 USD if no limit is explicitly set).
- Mark transactions exceeding the available monthly limit with a technical flag (`limit_exceeded = true`) in real time, handling concurrency safely via pessimistic database locks.
- Provide a client-facing API to set new limits, list existing limits, and fetch all transactions that exceeded their limits.
- Expose Model Context Protocol (MCP) servers (PostgreSQL Read-Only and Spring AI Client API) for AI Agent integration.

---

## Technology Stack
- **Language & Runtime:** Java 21 (Virtual Threads enabled, Records, Pattern Matching).
- **Framework:** Spring Boot 4 (spring-boot-starter-web, Spring Data JPA, Spring AI MCP).
- **Data Access Layer:** jOOQ (for complex SQL queries, CTEs, JOINs, and `FOR UPDATE` pessimistic locks), Liquibase (YAML migrations, Schema-First approach).
- **Database & Caching:** PostgreSQL 15, Redis (StringRedisTemplate for fast rate caching).
- **Integrations:** OpenFeign, Resilience4j (Circuit Breaker, Bulkhead, Retry), Spring AI MCP (`spring-ai-mcp-server-spring-boot-starter`).
- **Mappers & Utilities:** MapStruct, Lombok.
- **API Documentation & Validation:** Springdoc OpenAPI (Swagger UI), Jakarta Bean Validation (RFC 7807 ProblemDetail).
- **Testing:** JUnit 5, Mockito, Testcontainers (PostgreSQL, Redis).

---

## AI Agent Commands & Runbook

AI agents working in this repository MUST use the following explicit terminal commands for building, testing, and managing local environment components:

### 1. Compilation & Verification
- **Compile project:**
  ```powershell
  .\mvnw.cmd compile -DskipTests
  ```
- **Run full test suite (JUnit 5 + Testcontainers):**
  ```powershell
  .\mvnw.cmd test
  ```
- **Run a single test class:**
  ```powershell
  .\mvnw.cmd test -Dtest=TransactionExecutionServiceTest
  ```

### 2. Infrastructure & Docker Compose
- **Start PostgreSQL and Redis services:**
  ```powershell
  docker compose up -d postgres redis
  ```
- **Start full application stack via Docker:**
  ```powershell
  docker compose up -d
  ```
- **Stop local containers:**
  ```powershell
  docker compose down
  ```

### 3. Database Administration & Read-Only User Setup
- **Provision `readonly_user` for PostgreSQL MCP server:**
  ```powershell
  docker exec -i transaction_validation_postgres psql -U postgres -d transaction_validation_service_db -v READONLY_PASS="$DB_READONLY_PASSWORD" < scripts/create_readonly_user.sql
  ```

### 4. Running the Spring Boot Application
- **Run application locally:**
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
- **App Port:** `8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **Spring AI MCP Endpoint:** `http://localhost:8080/mcp/sse`

---

## Business Rules & Domain Logic

### Limit Management
- **Categories:** `product` (goods) and `service` (services). Limits and expenses are accumulated separately per category.
- **Default Limit:** If no custom limit has been set by the client, it defaults to 1000.00 USD.
- **Setting a New Limit:** When creating a new limit, the creation timestamp is automatically set to the current system time using `java.time.Clock`. Setting limit timestamps in the past or future is strictly prohibited. Updating (`UPDATE`) existing limits is forbidden. Every new limit must be inserted as a new historical record.
- **Historical Consistency:** Setting a new limit must not alter the `limit_exceeded` flag of transactions performed prior to the timestamp of the new limit. Monthly accumulated expenses calculation includes all transactions within the current month, but checks against the effective limit active at the time of each transaction.

### Time Zone & Monthly Boundaries
- All transactions and limits are normalized to a single system calculation time zone: `Europe/Moscow`.
- Monthly accounting boundaries run from `00:00:00.000` on the 1st day of the month to `23:59:59.999` on the last day of the month in the `Europe/Moscow` time zone.
- All current time evaluations in service logic and tests must be executed strictly via a `java.time.Clock` bean to allow time-travel simulation in integration tests.

### Foreign Exchange Rates (FX Integration)
- **External Provider:** TwelveData API (`/time_series?interval=1day`).
- **Closing Rate:** Calculations must use the close rate for the transaction date.
- **Fallback & Holiday Logic:** Check Redis cache first, then the local `currency_rates` table. If missing locally, fetch from TwelveData API. If unavailable (weekends/holidays) or if API fails, fetch the latest available close rate (`previous_close`) from the local database on `date < transaction_date`.
- **Resilience:** External API calls are wrapped in Resilience4j Circuit Breaker, Bulkhead, and Retry mechanisms.

### Concurrency & Thread Safety
- Concurrent requests for the same account and category must not read the same remaining limit balance.
- **Concurrency Strategy:** Execute `acquireClientLock()` in `TransactionJooqRepository` to insert/upsert into `limit_locks` AND execute a pessimistic `SELECT ... FOR UPDATE` row lock on `limit_locks` within a single database transaction (`TransactionExecutionService`).

---

## Model Context Protocol (MCP) Integrations

The service provides two MCP servers configured in [`.agents/mcp_config.json`](file:///c:/Projects/TransactionValidationService/.agents/mcp_config.json):

1. **`postgres-readonly` (PostgreSQL Database Agent)**:
   - Command: `@modelcontextprotocol/server-postgres` via `npx`.
   - Uses environment variables `PGUSER` (`${DB_READONLY_USER}`) and `PGPASSWORD` (`${DB_READONLY_PASSWORD}`).
   - Allows safe read-only SQL inspection of DB schema and data.

2. **`client-api-mcp` (Spring AI Client API Agent)**:
   - Endpoint: `http://localhost:8080/mcp/sse`.
   - Component: `ClientMcpTools` exposing `getClientLimits` and `getExceededTransactions`.

Skill reference runbook available at [`.agents/skills/postgres-client-mcp/SKILL.md`](file:///c:/Projects/TransactionValidationService/.agents/skills/postgres-client-mcp/SKILL.md).

---

## REST API Specification

### Integration API (Internal Processing)
* **`POST` `/api/v1/transactions`** — Ingest incoming transactions.
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

### Client API (External Banking App)
* **`POST` `/api/v1/client/limits`** — Set a new expense limit.
  ```json
  {
    "account_from": "0000000123",
    "expense_category": "product",
    "limit_sum": 1500.00
  }
  ```
* **`GET` `/api/v1/client/limits?account_from=0000000123`** — Fetch all historical limits for a client.
* **`GET` `/api/v1/client/transactions/exceeded?account_from=0000000123`** — Fetch transactions that exceeded limits.

---

## Database Model (PostgreSQL / Liquibase Schema)
- **`expense_limits`:** Stores historical expense limit records.
- **`transactions`:** Stores ingested transactions with calculated USD equivalents and foreign key `applied_limit_id`.
- **`currency_rates`:** Caches exchange rate close values (KZT/USD, RUB/USD).
- **`limit_locks`:** Lock table used for pessimistic concurrency control per `(account_number, expense_category)`.

---

## Testing Strategy
- **Infrastructure:** Real PostgreSQL instances spun up via Testcontainers (`@ServiceConnection`).
- **External Mocking:** External API calls stubbed via WireMock in integration tests.
- **Code Cleanliness:** Magic strings, IDs, dates, and amounts centralized in `TestConstants` utility class.