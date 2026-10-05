package by.ares.transaction_validation_service.service;

import by.ares.transaction_validation_service.exception.FxRateNotFoundException;
import by.ares.transaction_validation_service.model.CurrencyRate;
import by.ares.transaction_validation_service.repository.CurrencyRateRepository;
import by.ares.transaction_validation_service.service.impl.CurrencyRateServiceImpl;
import by.ares.transaction_validation_service.service.impl.TwelveDataSyncServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static by.ares.transaction_validation_service.TestConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrencyRateServiceImplTest {

    @Mock
    private CurrencyRateRepository currencyRateRepository;
    @Mock
    private TwelveDataSyncServiceImpl twelveDataSyncService;
    @Mock
    private RateCachingService cachingService;

    @InjectMocks
    private CurrencyRateServiceImpl currencyRateService;

    @Test
    void shouldReturnOneImmediatelyWhenCurrencyIsUsd() {
        var result = currencyRateService.getCloseRate(CURRENCY_CODE_USD, TEST_DATE);
        assertEquals(BigDecimal.ONE, result);
        verifyNoInteractions(currencyRateRepository, twelveDataSyncService, cachingService);
    }

    @Test
    void shouldReturnRateFromCacheWhenCacheHit() {
        when(cachingService.getCachedRate(PAIR_KZT_USD, TEST_DATE)).thenReturn(RATE_480_00);
        var result = currencyRateService.getCloseRate(CURRENCY_CODE_KZT, TEST_DATE);
        assertEquals(RATE_480_00, result);
        verify(cachingService).getCachedRate(PAIR_KZT_USD, TEST_DATE);
        verifyNoInteractions(currencyRateRepository, twelveDataSyncService);
    }

    @Test
    void shouldReturnRateFromDbAndCacheItWhenCacheMissButDbHit() {
        var existingRate = CurrencyRate.builder().closeRate(RATE_480_00).build();
        when(cachingService.getCachedRate(PAIR_KZT_USD, TEST_DATE)).thenReturn(null);
        when(currencyRateRepository.findByCurrencyPairAndRateDate(PAIR_KZT_USD, TEST_DATE))
                .thenReturn(Optional.of(existingRate));

        var result = currencyRateService.getCloseRate(CURRENCY_CODE_KZT, TEST_DATE);

        assertEquals(RATE_480_00, result);
        verify(cachingService).cacheRate(PAIR_KZT_USD, TEST_DATE, RATE_480_00);
        verifyNoInteractions(twelveDataSyncService);
    }

    @Test
    void shouldSyncAndReturnRateWhenRateNotInCacheOrDbInitially() {
        when(cachingService.getCachedRate(PAIR_KZT_USD, TEST_DATE))
                .thenReturn(null)
                .thenReturn(RATE_480_00);
        when(currencyRateRepository.findByCurrencyPairAndRateDate(PAIR_KZT_USD, TEST_DATE))
                .thenReturn(Optional.empty());

        var result = currencyRateService.getCloseRate(CURRENCY_CODE_KZT, TEST_DATE);

        assertEquals(RATE_480_00, result);
        verify(twelveDataSyncService).syncRates(PAIR_KZT_USD);
        verify(cachingService, times(2)).getCachedRate(PAIR_KZT_USD, TEST_DATE);
    }

    @Test
    void shouldUseFallbackRateAndCacheItWhenExternalSyncFails() {
        var fallbackRate = CurrencyRate.builder().closeRate(RATE_479_50).build();
        when(cachingService.getCachedRate(PAIR_KZT_USD, TEST_DATE)).thenReturn(null);
        when(currencyRateRepository.findByCurrencyPairAndRateDate(PAIR_KZT_USD, TEST_DATE)).thenReturn(Optional.empty());
        doThrow(new FxRateNotFoundException(API_TIMEOUT_MSG)).when(twelveDataSyncService).syncRates(PAIR_KZT_USD);
        when(currencyRateRepository.findTopByCurrencyPairAndRateDateLessThanEqualOrderByRateDateDesc(PAIR_KZT_USD, TEST_DATE))
                .thenReturn(Optional.of(fallbackRate));

        var result = currencyRateService.getCloseRate(CURRENCY_CODE_KZT, TEST_DATE);

        assertEquals(RATE_479_50, result);
        verify(twelveDataSyncService).syncRates(PAIR_KZT_USD);
        verify(cachingService).cacheRate(PAIR_KZT_USD, TEST_DATE, RATE_479_50);
    }

    @Test
    void shouldThrowExceptionWhenSyncFailsAndNoFallbackExists() {
        when(cachingService.getCachedRate(PAIR_KZT_USD, TEST_DATE)).thenReturn(null);
        when(currencyRateRepository.findByCurrencyPairAndRateDate(PAIR_KZT_USD, TEST_DATE)).thenReturn(Optional.empty());
        doThrow(new FxRateNotFoundException(API_TIMEOUT_MSG)).when(twelveDataSyncService).syncRates(PAIR_KZT_USD);
        when(currencyRateRepository.findTopByCurrencyPairAndRateDateLessThanEqualOrderByRateDateDesc(PAIR_KZT_USD, TEST_DATE))
                .thenReturn(Optional.empty());

        assertThrows(FxRateNotFoundException.class, () -> currencyRateService.getCloseRate(CURRENCY_CODE_KZT, TEST_DATE));
    }
}