package by.ares.transaction_validation_service.service.impl;

import by.ares.transaction_validation_service.dto.TwelveDataResponseDto;
import by.ares.transaction_validation_service.exception.FxRateNotFoundException;
import by.ares.transaction_validation_service.feign.TwelveDataClient;
import by.ares.transaction_validation_service.model.CurrencyRate;
import by.ares.transaction_validation_service.repository.CurrencyRateRepository;
import by.ares.transaction_validation_service.service.TwelveDataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwelveDataSyncServiceImpl implements TwelveDataSyncService {

    private final TwelveDataClient twelveDataClient;
    private final CurrencyRateRepository currencyRateRepository;

    @Value("${integration.twelvedata.api-key:test-key}")
    private String apiKey;

    @Override
    public void syncRates(String pair) {
        log.info("Fetching FX rate for pair {} from external API", pair);
        String externalSymbol = "USD/" + pair.split("/")[0];
        var response = twelveDataClient.getExchangeRate(externalSymbol, "1day", 5, apiKey);
        if (isEmpty(response)) {
            throw new FxRateNotFoundException("Empty response from external FX API for " + pair);
        }
        var responseDates = response.values()
                .stream()
                .map(val -> LocalDate.parse(val.datetime()))
                .toList();
        var existingDates = currencyRateRepository.findExistingDates(pair, responseDates);
        var currencyRates = response.values()
                .stream()
                .map(val -> {
                    LocalDate rateDate = LocalDate.parse(val.datetime());
                    if (existingDates.contains(rateDate)) {
                        return null;
                    }
                    var usdToCurrency = new BigDecimal(val.close());
                    var closestRate = BigDecimal.ONE
                            .divide(usdToCurrency, 6, RoundingMode.HALF_UP);

                    return CurrencyRate.builder()
                            .currencyPair(pair)
                            .rateDate(rateDate)
                            .closeRate(closestRate)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
        if (!currencyRates.isEmpty()) {
            currencyRateRepository.saveAll(currencyRates);
            log.info("Saved {} new rates for {}", currencyRates.size(), pair);
        }
    }

    private boolean isEmpty(TwelveDataResponseDto response) {
        return response == null || response.values() == null || response.values().isEmpty();
    }
}