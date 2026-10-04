package by.ares.transaction_validation_service.feign.fallback;

import by.ares.transaction_validation_service.exception.ApiException;
import by.ares.transaction_validation_service.exception.ApiTimeoutException;
import by.ares.transaction_validation_service.feign.TwelveDataClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import static by.ares.transaction_validation_service.util.TransactionValidationServiceConst.API_TIMEOUT_MESSAGE;

@Component
public class TwelveDataClientFallback implements FallbackFactory<TwelveDataClient> {

    @Override
    public TwelveDataClient create(Throwable cause) {
        if (cause instanceof ApiException apiException) {
            throw apiException;
        }
        return (symbol, interval, outputsize, apiKey) -> {
            throw new ApiTimeoutException(API_TIMEOUT_MESSAGE);
        };
    }
}