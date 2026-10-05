---
name: postgres-client-mcp
description: >-
  Comprehensive guide and runbook for configuring PostgreSQL Read-Only MCP server and
  Spring AI Client API MCP server in TransactionValidationService.
---

# PostgreSQL & Client API MCP Integration Skill

This skill documents the complete setup, configuration, and tool usage for Model Context Protocol (MCP) servers in `TransactionValidationService`.

---

## 1. Architecture Overview

`TransactionValidationService` integrates two MCP server channels for AI agents:

1. **PostgreSQL Read-Only MCP Server (`postgres-readonly`)**:
   - Uses `@modelcontextprotocol/server-postgres` via Stdio.
   - Connects to PostgreSQL using environment variable authentication (`PGUSER` / `PGPASSWORD`), preventing hardcoded plaintext secrets in source files.
   - Allows AI agents to safely read DB schema (`expense_limits`, `transactions`, `currency_rates`, `limit_locks`) and query data without data modification risk.

2. **Spring AI Client API MCP Server (`client-api-mcp`)**:
   - Built into Spring Boot using `spring-ai-mcp-server-spring-boot-starter` (`1.0.0-M6`).
   - Exposes SSE endpoints at `http://localhost:8080/mcp/sse`.
   - Exposes client domain tools:
     - `getClientLimits`: Fetches historical expense limits for an account.
     - `getExceededTransactions`: Fetches transactions that exceeded monthly limits.

---

## 2. PostgreSQL Read-Only MCP Setup

### Step 1: Create `readonly_user` in PostgreSQL
Execute [scripts/create_readonly_user.sql](file:///c:/Projects/TransactionValidationService/scripts/create_readonly_user.sql) against the database using environment variable parameterization:

```sql
DO $$
DECLARE
    pwd text := :'READONLY_PASS';
BEGIN
    IF pwd IS NULL OR pwd = '' OR pwd = ':\'READONLY_PASS\'' THEN
        pwd := 'readonly_pass';
    END IF;

    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'readonly_user') THEN
        EXECUTE format('CREATE USER readonly_user WITH PASSWORD %L', pwd);
    ELSE
        EXECUTE format('ALTER USER readonly_user WITH PASSWORD %L', pwd);
    END IF;
END
$$;

GRANT CONNECT ON DATABASE transaction_validation_service_db TO readonly_user;
GRANT USAGE ON SCHEMA public TO readonly_user;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO readonly_user;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO readonly_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO readonly_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON SEQUENCES TO readonly_user;
```

### Docker Execution Command (Injecting Password):
```bash
docker exec -i transaction_validation_postgres psql -U postgres -d transaction_validation_service_db -v READONLY_PASS="$DB_READONLY_PASSWORD" < scripts/create_readonly_user.sql
```

---

## 3. Spring AI Client API MCP Implementation

### Maven Dependencies ([pom.xml](file:///c:/Projects/TransactionValidationService/pom.xml))
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-spring-boot-starter</artifactId>
</dependency>
```

### Tool Component ([ClientMcpTools.java](file:///c:/Projects/TransactionValidationService/src/main/java/by/ares/transaction_validation_service/mcp/ClientMcpTools.java))
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ClientMcpTools {

    private final ExpenseLimitService expenseLimitService;
    private final TransactionProcessingService transactionProcessingService;

    @Tool(description = "Fetch all historical expense limits for a specific client account number")
    public List<ExpenseLimitDto> getClientLimits(
            @ToolParam(description = "The client account number (account_from)") String accountFrom) {
        return expenseLimitService.getClientLimits(accountFrom);
    }

    @Tool(description = "Fetch all transactions that exceeded the monthly limit for a specific client account number")
    public List<ExceededTransactionResponseDto> getExceededTransactions(
            @ToolParam(description = "The client account number (account_from)") String accountFrom) {
        return transactionProcessingService.getExceededTransactions(accountFrom);
    }
}
```

### Configuration ([application.yaml](file:///c:/Projects/TransactionValidationService/src/main/resources/application.yaml))
```yaml
spring:
  ai:
    mcp:
      server:
        enabled: true
        name: client-api-mcp-server
        version: 1.0.0
        transport: sse
        sse:
          path: /mcp/sse
          message-path: /mcp/message
```

---

## 4. MCP Workspace Registration

Workspace configuration file at [`.agents/mcp_config.json`](file:///c:/Projects/TransactionValidationService/.agents/mcp_config.json):

```json
{
  "mcpServers": {
    "postgres-readonly": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-postgres",
        "${DB_READONLY_URL:-postgresql://localhost:5433/transaction_validation_service_db}"
      ],
      "env": {
        "PGUSER": "${DB_READONLY_USER:-readonly_user}",
        "PGPASSWORD": "${DB_READONLY_PASSWORD}",
        "PGSSLMODE": "disable"
      }
    },
    "client-api-mcp": {
      "serverUrl": "http://localhost:8080/mcp/sse"
    }
  }
}
```

---

## 5. Usage & Verification

1. Start PostgreSQL (Docker): `docker compose up -d postgres`
2. Set read-only user password environment variable: `export DB_READONLY_PASSWORD="your_secure_password"`
3. Apply read-only user script: `docker exec -i transaction_validation_postgres psql -U postgres -d transaction_validation_service_db -v READONLY_PASS="$DB_READONLY_PASSWORD" < scripts/create_readonly_user.sql`
4. Run Spring Boot application: `./mvnw.cmd spring-boot:run`
5. AI agents automatically discover tools in `.agents/mcp_config.json`:
   - `postgres-readonly`: Inspect tables (`expense_limits`, `transactions`, `currency_rates`), read schemas & execute safe queries.
   - `client-api-mcp`: Directly call `getClientLimits` and `getExceededTransactions`.
