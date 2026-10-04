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

import static by.ares.transaction_validation_service.util.TransactionValidationServiceConst.DEFAULT_CURRENCY_CODE;
import static by.ares.transaction_validation_service.util.TransactionValidationServiceConst.FX_RATE_NOT_FOUND_MESSAGE;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyRateServiceImpl implements CurrencyRateService {

    private final CurrencyRateRepository currencyRateRepository;
    private final TwelveDataSyncServiceImpl twelveDataSyncService;

    @Override
    public BigDecimal getCloseRate(String currency, LocalDate date) {
        if (DEFAULT_CURRENCY_CODE.equalsIgnoreCase(currency)) {
            return BigDecimal.ONE;
        }
        var pair = currency.toUpperCase() + "/" + DEFAULT_CURRENCY_CODE;
        var localRate = currencyRateRepository.findByCurrencyPairAndRateDate(pair, date);
        if (localRate.isPresent()) {
            return localRate.get().getCloseRate();
        }
        log.warn("Rate for {} on {} not found in DB. Triggering on-the-fly sync.", pair, date);
        try {
            twelveDataSyncService.syncRates(pair);
        } catch (Exception e) {
            log.error("External API sync failed for {}: {}", pair, e.getMessage());
        }
        return currencyRateRepository.findByCurrencyPairAndRateDate(pair, date)
                .map(CurrencyRate::getCloseRate)
                .orElseGet(() -> getFallbackRate(pair, date));
    }

    private BigDecimal getFallbackRate(String pair, LocalDate date) {
        log.warn("FX rate missing for {}. Executing Fallback to previous close.", pair);
        return currencyRateRepository.findTopByCurrencyPairAndRateDateLessThanEqualOrderByRateDateDesc(pair, date)
                .map(CurrencyRate::getCloseRate)
                .orElseThrow(() -> new FxRateNotFoundException(
                        FX_RATE_NOT_FOUND_MESSAGE + pair));
    }
}