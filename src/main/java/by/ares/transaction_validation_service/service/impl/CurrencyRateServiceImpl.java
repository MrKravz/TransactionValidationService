package by.ares.transaction_validation_service.service.impl;

import by.ares.transaction_validation_service.exception.FxRateNotFoundException;
import by.ares.transaction_validation_service.model.CurrencyRate;
import by.ares.transaction_validation_service.repository.CurrencyRateRepository;
import by.ares.transaction_validation_service.service.RateCachingService;
import by.ares.transaction_validation_service.service.CurrencyRateService;
import by.ares.transaction_validation_service.service.CurrencySyncService;
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
    private final CurrencySyncService currencySyncService;
    private final RateCachingService rateCachingService;

    @Override
    public BigDecimal getCloseRate(String currency, LocalDate date) {
        if (DEFAULT_CURRENCY_CODE.equalsIgnoreCase(currency)) {
            return BigDecimal.ONE;
        }
        var pair = currency.toUpperCase() + "/" + DEFAULT_CURRENCY_CODE;
        BigDecimal cachedRate = rateCachingService.getCachedRate(pair, date);
        if (cachedRate != null) {
            log.debug("FX rate for {} on {} found in Redis Cache", pair, date);
            return cachedRate;
        }
        var localRateOpt = currencyRateRepository.findByCurrencyPairAndRateDate(pair, date);
        if (localRateOpt.isPresent()) {
            BigDecimal rate = localRateOpt.get().getCloseRate();
            log.debug("FX rate for {} on {} found in DB. Caching it.", pair, date);
            rateCachingService.cacheRate(pair, date, rate);
            return rate;
        }
        log.warn("Rate for {} on {} not found in DB. Triggering on-the-fly sync.", pair, date);
        try {
            currencySyncService.syncRates(pair);
        }
        catch (Exception e) {
            log.error("External API sync failed for {}: {}", pair, e.getMessage());
        }
        cachedRate = rateCachingService.getCachedRate(pair, date);
        if (cachedRate != null) {
            return cachedRate;
        }
        return getFallbackRateAndCache(pair, date);
    }

    private BigDecimal getFallbackRateAndCache(String pair, LocalDate requestedDate) {
        log.warn("FX rate missing for {} on {}. Executing Fallback to previous close.", pair, requestedDate);
        BigDecimal fallbackRate = currencyRateRepository
                .findTopByCurrencyPairAndRateDateLessThanEqualOrderByRateDateDesc(pair, requestedDate)
                .map(CurrencyRate::getCloseRate)
                .orElseThrow(() -> new FxRateNotFoundException(FX_RATE_NOT_FOUND_MESSAGE + pair));
        rateCachingService.cacheRate(pair, requestedDate, fallbackRate);
        return fallbackRate;
    }
}