# Skill for generating new feign clients to communicate with different currency providers API

## name: currency-feign-client-generator
## description: Generates Spring Cloud OpenFeign clients, FallbackFactory classes, and DTOs for currency rate provider integrations. Use when asked to create or generate Feign clients for exchange rate APIs, FX providers, or rate services based on existing project conventions.

### Currency Feign Client Generator
A generator for Spring Cloud OpenFeign client layers, fault-tolerance fallback factories (FallbackFactory), and DTOs for currency rate provider integrations, following the project's architectural standards.

### When to Use
Integrating a new currency rate provider (e.g., Fixer, Alpha Vantage, ExchangeRatesAPI, CoinGecko).

Generating Feign clients with Resilience4j support (@Retry, @Bulkhead) and custom fallback factories.

### Architectural Patterns
#### Feign Client interface
``` Java
package .feign;

import .dto.ResponseDto;
import .feign.fallback.ClientFallback;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
name = "",
contextId = "",
url = "${feign.client.api.url:}",
fallbackFactory = ClientFallback.class
)
@Retry(name = "")
@Bulkhead(name = "")
public interface Client {

    @GetMapping("")
    ResponseDto (
            @RequestParam("symbol") String symbol,
            @RequestParam("interval") String interval,
            @RequestParam("outputsize") int outputSize,
            @RequestParam("apikey") String apiKey
    );
}
```
#### FallbackFactory implementation
``` Java
package .feign.fallback;

import .feign.Client;
import .exception.ApiException;
import .exception.ApiTimeoutException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ClientFallback implements FallbackFactory<Client> {

    private static final String API_TIMEOUT_MESSAGE = "API request timed out or service unavailable";

    @Override
    public Client create(Throwable cause) {
        if (cause instanceof ApiException apiException) {
            throw apiException;
        }
        return () -> {
            throw new ApiTimeoutException(API_TIMEOUT_MESSAGE);
        };
    }
}
```
#### Response DTO
``` Java
package .dto;

public record ResponseDto(
// API response fields
) {}
```

### Generation Instructions
#### When receiving a request to create a new client:
##### Define Provider Parameters
- **API Name (name):** Canonical provider name in kebab-case (e.g., fixer-api, alpha-vantage-api).
- **Context ID (contextId):** Purpose identifier for the client (e.g., currency-read-client, crypto-rate-client).
- **Base URL and Endpoints:** Configured according to the third-party API specification.
##### Generate Components
- **Feign Client Interface:** Annotate with @FeignClient, @Retry, and @Bulkhead.
- **Fallback Factory:** Annotate with @Component, implement FallbackFactory, rethrow ApiException, and throw ApiTimeoutException on timeouts/failures.
- **Response DTO:** Create a Java record or class with JSON mapping annotations (Jackson) matching the provider's response payload structure.
- **Configuration:** Provide an application.yml sample snippet with Resilience4j and Feign settings if required.