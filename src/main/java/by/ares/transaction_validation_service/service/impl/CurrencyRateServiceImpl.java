package by.ares.transaction_validation_service.service.impl;

import by.ares.transaction_validation_service.exception.FxRateNotFoundException;
import by.ares.transaction_validation_service.model.CurrencyRate;
import by.ares.transaction_validation_service.repository.CurrencyRateRepository;
import by.ares.transaction_validation_service.service.CurrencyRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyRateServiceImpl implements CurrencyRateService {

    private final CurrencyRateRepository currencyRateRepository;
    private final TwelveDataSyncServiceImpl twelveDataSyncService;

    @Override
    public BigDecimal getCloseRate(String currency, LocalDate date) {
        if ("USD".equalsIgnoreCase(currency)) {
            return BigDecimal.ONE;
        }
        var pair = currency.toUpperCase() + "/USD";
        var localRate = currencyRateRepository.findByCurrencyPairAndRateDate(pair, date);
        if (localRate.isPresent()) {
            return localRate.get().getCloseRate();
        }
        log.warn("Rate for {} on {} not found in DB. Triggering on-the-fly sync.", pair, date);
        try {
            twelveDataSyncService.syncRates(pair);
            return currencyRateRepository.findByCurrencyPairAndRateDate(pair, date)
                    .map(CurrencyRate::getCloseRate)
                    .orElseThrow(() -> new FxRateNotFoundException("API responded successfully, but rate for specific date is missing"));

        } catch (Exception e) {
            return getFallbackRate(pair, date, e);
        }
    }

    private BigDecimal getFallbackRate(String pair, LocalDate date, Exception e) {
        log.warn("FX API unavailable for {}. Executing Fallback to previous close. Error: {}", pair, e.getMessage());
        return currencyRateRepository.findTopByCurrencyPairAndRateDateLessThanEqualOrderByRateDateDesc(pair, date)
                .map(CurrencyRate::getCloseRate)
                .orElseThrow(() -> new FxRateNotFoundException(
                        "Critical Error: FX API is down and NO fallback rate found in DB for " + pair));
    }
}