package by.ares.transaction_validation_service.feign;

import by.ares.transaction_validation_service.dto.TwelveDataResponseDto;
import by.ares.transaction_validation_service.feign.fallback.TwelveDataClientFallback;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "twelve-data-api",
        contextId = "currency-read-client",
        url = "${feign.client.api.url:https://api.twelvedata.com}",
        fallbackFactory = TwelveDataClientFallback.class
)
@Retry(name = "twelve-data-api")
@Bulkhead(name = "twelve-data-api")
public interface TwelveDataClient {

    @GetMapping("/time_series")
    TwelveDataResponseDto getExchangeRate(@RequestParam("symbol") String symbol, @RequestParam("interval") String interval,
            @RequestParam("outputsize") int outputSize, @RequestParam("apikey") String apiKey);
}