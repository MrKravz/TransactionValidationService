package by.ares.transaction_validation_service.service;

import by.ares.transaction_validation_service.dto.TwelveDataResponseDto;
import by.ares.transaction_validation_service.exception.FxRateNotFoundException;
import by.ares.transaction_validation_service.feign.TwelveDataClient;
import by.ares.transaction_validation_service.model.CurrencyRate;
import by.ares.transaction_validation_service.repository.CurrencyRateRepository;
import by.ares.transaction_validation_service.service.impl.TwelveDataSyncServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static by.ares.transaction_validation_service.TestConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwelveDataSyncServiceImplTest {

    @Mock
    private TwelveDataClient twelveDataClient;
    @Mock
    private CurrencyRateRepository currencyRateRepository;

    @InjectMocks
    private TwelveDataSyncServiceImpl syncService;

    @Captor
    private ArgumentCaptor<List<CurrencyRate>> ratesCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(syncService, "apiKey", API_KEY);
    }

    @Test
    void shouldSuccessfullySyncAndSaveOnlyNewRates() {
        TwelveDataResponseDto response = new TwelveDataResponseDto(
                new TwelveDataResponseDto.MetaDto(PAIR_USD_KZT, INTERVAL_1DAY),
                List.of(new TwelveDataResponseDto.ValueDto(
                                DATE_OCT_01_STR,
                                RATE_480_00.toString(),
                                RATE_480_00.toString(),
                                RATE_480_00.toString(),
                                RATE_480_00.toString()),
                        new TwelveDataResponseDto.ValueDto(
                                DATE_OCT_02_STR,
                                RATE_481_00.toString(),
                                RATE_481_00.toString(),
                                RATE_481_00.toString(),
                                RATE_481_00.toString())),
                STATUS_OK
        );
        when(twelveDataClient.getExchangeRate(PAIR_USD_KZT, INTERVAL_1DAY, OUTPUT_SIZE_5, API_KEY)).thenReturn(response);
        when(currencyRateRepository.findExistingDates(eq(PAIR_KZT_USD), anyList())).thenReturn(Set.of(DATE_OCT_02));
        syncService.syncRates(PAIR_KZT_USD);
        verify(currencyRateRepository).saveAll(ratesCaptor.capture());
        List<CurrencyRate> savedRates = ratesCaptor.getValue();
        assertEquals(1, savedRates.size());
        assertEquals(DATE_OCT_01, savedRates.getFirst().getRateDate());
        assertEquals(PAIR_KZT_USD, savedRates.getFirst().getCurrencyPair());
        assertEquals(RATE_480_00, savedRates.getFirst().getCloseRate());
    }

    @Test
    void shouldThrowExceptionWhenResponseValuesListIsEmpty() {
        var emptyResponse = new TwelveDataResponseDto(new TwelveDataResponseDto.MetaDto(PAIR_USD_KZT,
                INTERVAL_1DAY), Collections.emptyList(), STATUS_ERROR);
        when(twelveDataClient.getExchangeRate(PAIR_USD_KZT, INTERVAL_1DAY, OUTPUT_SIZE_5, API_KEY)).thenReturn(emptyResponse);
        assertThrows(FxRateNotFoundException.class, () -> syncService.syncRates(PAIR_KZT_USD));
        verify(currencyRateRepository, never()).saveAll(any());
    }

    @Test
    void shouldThrowExceptionWhenResponseIsNull() {
        when(twelveDataClient.getExchangeRate(PAIR_USD_KZT, INTERVAL_1DAY, OUTPUT_SIZE_5, API_KEY)).thenReturn(null);
        assertThrows(FxRateNotFoundException.class, () -> syncService.syncRates(PAIR_KZT_USD));
        verify(currencyRateRepository, never()).saveAll(any());
    }
}