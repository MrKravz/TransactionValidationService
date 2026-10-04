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
import java.time.LocalDate;
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

    @InjectMocks
    private CurrencyRateServiceImpl currencyRateService;

    @Test
    void shouldReturnOneImmediatelyWhenCurrencyIsUsd() {
        var result = currencyRateService.getCloseRate(CURRENCY_CODE_USD, LocalDate.now());
        assertEquals(BigDecimal.ONE, result);
        verifyNoInteractions(currencyRateRepository, twelveDataSyncService);
    }

    @Test
    void shouldReturnRateFromDbWhenRateExists() {
        var existingRate = CurrencyRate.builder().closeRate(RATE_480_00).build();
        when(currencyRateRepository.findByCurrencyPairAndRateDate(PAIR_KZT_USD, TEST_DATE))
                .thenReturn(Optional.of(existingRate));
        var result = currencyRateService.getCloseRate(CURRENCY_CODE_KZT, TEST_DATE);
        assertEquals(RATE_480_00, result);
        verifyNoInteractions(twelveDataSyncService);
    }

    @Test
    void shouldSyncAndReturnRateWhenRateNotInDbInitially() {
        var syncedRate = CurrencyRate.builder().closeRate(RATE_480_00).build();
        when(currencyRateRepository.findByCurrencyPairAndRateDate(PAIR_KZT_USD, TEST_DATE)).thenReturn(Optional.empty())
                .thenReturn(Optional.of(syncedRate));
        var result = currencyRateService.getCloseRate(CURRENCY_CODE_KZT, TEST_DATE);
        assertEquals(RATE_480_00, result);
        verify(twelveDataSyncService).syncRates(PAIR_KZT_USD);
        verify(currencyRateRepository, times(2)).findByCurrencyPairAndRateDate(PAIR_KZT_USD, TEST_DATE);
    }

    @Test
    void shouldThrowExceptionWhenSyncSucceedsButRateStillMissing() {
        when(currencyRateRepository.findByCurrencyPairAndRateDate(PAIR_KZT_USD, TEST_DATE)).thenReturn(Optional.empty());
        doNothing().when(twelveDataSyncService).syncRates(PAIR_KZT_USD);
        assertThrows(FxRateNotFoundException.class, () -> currencyRateService.getCloseRate(CURRENCY_CODE_KZT, TEST_DATE));
        verify(twelveDataSyncService).syncRates(PAIR_KZT_USD);
        verify(currencyRateRepository, atLeastOnce()).findByCurrencyPairAndRateDate(PAIR_KZT_USD, TEST_DATE);
    }

    @Test
    void shouldUseFallbackRateWhenExternalSyncFails() {
        var fallbackRate = CurrencyRate.builder().closeRate(RATE_479_50).build();
        when(currencyRateRepository.findByCurrencyPairAndRateDate(PAIR_KZT_USD, TEST_DATE)).thenReturn(Optional.empty());
        doThrow(new FxRateNotFoundException(API_TIMEOUT_MSG)).when(twelveDataSyncService).syncRates(PAIR_KZT_USD);
        when(currencyRateRepository.findTopByCurrencyPairAndRateDateLessThanEqualOrderByRateDateDesc(PAIR_KZT_USD, TEST_DATE))
                .thenReturn(Optional.of(fallbackRate));
        var result = currencyRateService.getCloseRate(CURRENCY_CODE_KZT, TEST_DATE);
        assertEquals(RATE_479_50, result);
        verify(twelveDataSyncService).syncRates(PAIR_KZT_USD);
        verify(currencyRateRepository).findTopByCurrencyPairAndRateDateLessThanEqualOrderByRateDateDesc(PAIR_KZT_USD, TEST_DATE);
    }

    @Test
    void shouldThrowExceptionWhenSyncFailsAndNoFallbackExists() {
        when(currencyRateRepository.findByCurrencyPairAndRateDate(PAIR_KZT_USD, TEST_DATE)).thenReturn(Optional.empty());
        doThrow(new FxRateNotFoundException(API_TIMEOUT_MSG)).when(twelveDataSyncService).syncRates(PAIR_KZT_USD);
        when(currencyRateRepository.findTopByCurrencyPairAndRateDateLessThanEqualOrderByRateDateDesc(PAIR_KZT_USD, TEST_DATE))
                .thenReturn(Optional.empty());
        assertThrows(FxRateNotFoundException.class, () -> currencyRateService.getCloseRate(CURRENCY_CODE_KZT, TEST_DATE));
    }
}