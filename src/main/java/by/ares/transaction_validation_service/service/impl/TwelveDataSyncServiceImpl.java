package by.ares.transaction_validation_service.service.impl;

import by.ares.transaction_validation_service.dto.TwelveDataResponseDto;
import by.ares.transaction_validation_service.exception.FxRateNotFoundException;
import by.ares.transaction_validation_service.feign.TwelveDataClient;
import by.ares.transaction_validation_service.model.CurrencyRate;
import by.ares.transaction_validation_service.repository.CurrencyRateRepository;
import by.ares.transaction_validation_service.service.CurrencySyncService;
import by.ares.transaction_validation_service.service.RateCachingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

import static by.ares.transaction_validation_service.util.TransactionValidationServiceConst.DEFAULT_CURRENCY_CODE;
import static by.ares.transaction_validation_service.util.TransactionValidationServiceConst.EXCHANGE_RATE_INTERVAL;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwelveDataSyncServiceImpl implements CurrencySyncService {

    private final TwelveDataClient twelveDataClient;
    private final CurrencyRateRepository currencyRateRepository;
    private final RateCachingService rateCachingService;
    private final CurrencyRateStorageService currencyRateStorageService;

    @Value("${feign.client.api.key:test-key}")
    private String apiKey;

    @Override
    public void syncRates(String pair) {
        log.info("Fetching FX rate for pair {} from external API", pair);
        String externalSymbol = DEFAULT_CURRENCY_CODE + "/" + pair.split("/")[0];
        var response = twelveDataClient.getExchangeRate(externalSymbol, EXCHANGE_RATE_INTERVAL, 5, apiKey);

        if (isEmpty(response)) {
            throw new FxRateNotFoundException("Empty or invalid response from external FX API for " + pair);
        }

        Map<LocalDate, String> ratesFromApiMap = response.values()
                .stream()
                .collect(Collectors.toMap(
                        value -> LocalDate.parse(value.datetime()),
                        TwelveDataResponseDto.ValueDto::close,
                        (existing, replacement) -> existing
                ));

        var responseDates = ratesFromApiMap.keySet().stream().toList();
        var existingDates = currencyRateRepository.findExistingDates(pair, responseDates);

        var currencyRates = ratesFromApiMap.entrySet()
                .stream()
                .filter(entry -> !existingDates.contains(entry.getKey()))
                .map(entry -> CurrencyRate.builder()
                        .currencyPair(pair)
                        .rateDate(entry.getKey())
                        .closeRate(new BigDecimal(entry.getValue()))
                        .build())
                .collect(Collectors.toSet());

        currencyRateStorageService.saveCurrencyRates(currencyRates, pair);

        ratesFromApiMap.forEach((date, rateStr) ->
                rateCachingService.cacheRate(pair, date, new BigDecimal(rateStr))
        );
    }

    private boolean isEmpty(TwelveDataResponseDto response) {
        return response == null || response.values() == null || response.values().isEmpty();
    }
}