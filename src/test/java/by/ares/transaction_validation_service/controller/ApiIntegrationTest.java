package by.ares.transaction_validation_service.controller;

import by.ares.transaction_validation_service.AbstractIntegrationTest;
import by.ares.transaction_validation_service.dto.ExceededTransactionResponseDto;
import by.ares.transaction_validation_service.dto.SetLimitRequestDto;
import by.ares.transaction_validation_service.dto.TransactionRequestDto;
import by.ares.transaction_validation_service.dto.TransactionResponseDto;
import by.ares.transaction_validation_service.model.ExpenseCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static by.ares.transaction_validation_service.TestConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ApiIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private Clock clock;

    private RestClient restClient;

    @BeforeEach
    void setUp() throws Exception {
        this.restClient = RestClient.builder()
                .baseUrl(LOCALHOST_URL_PREFIX + port)
                .build();
        stubTwelveDataRates();
    }

    @Test
    void shouldCorrectlyValidateTransactionLimitsAndTrackExceededHistory() {
        setSystemTime(SYS_TIME_JAN_01);
        setClientLimit(LIMIT_1000);
        var transactionDto1 = sendTransaction(SUM_250_000, TX_DATE_JAN_02);
        assertFalse(transactionDto1.limitExceeded());
        var transactionDto2 = sendTransaction(SUM_300_000, TX_DATE_JAN_03);
        assertTrue(transactionDto2.limitExceeded());
        setSystemTime(SYS_TIME_JAN_10);
        setClientLimit(LIMIT_2000);
        var transactionDto3 = sendTransaction(SUM_50_000, TX_DATE_JAN_11);
        assertFalse(transactionDto3.limitExceeded());
        var transactionDto4 = sendTransaction(SUM_350_000, TX_DATE_JAN_12);
        assertFalse(transactionDto4.limitExceeded());
        var transactionDto5 = sendTransaction(SUM_50_000, TX_DATE_JAN_13_FIRST);
        assertFalse(transactionDto5.limitExceeded());
        var transactionDto6 = sendTransaction(SUM_50_000, TX_DATE_JAN_13_SECOND);
        assertTrue(transactionDto6.limitExceeded());
        var exceededTransactions = getExceededTransactions();
        assertNotNull(exceededTransactions);
        assertEquals(2, exceededTransactions.size());
        var firstExceeded = exceededTransactions.getFirst();
        assertEquals(SUM_300_000, firstExceeded.sum());
        assertEquals(LIMIT_1000, firstExceeded.limitSum());
        assertTrue(firstExceeded.limitDatetime().toString().contains(DATE_MATCH_JAN_01));
        var secondExceeded = exceededTransactions.get(1);
        assertEquals(SUM_50_000, secondExceeded.sum());
        assertEquals(LIMIT_2000, secondExceeded.limitSum());
        assertTrue(secondExceeded.limitDatetime().toString().contains(DATE_MATCH_JAN_10));
    }

    private List<ExceededTransactionResponseDto> getExceededTransactions() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(URI_GET_EXCEEDED_TRANSACTIONS)
                        .queryParam("account_from", ACCOUNT_NUMBER)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    private void setSystemTime(String utcTime) {
        var instant = Instant.parse(utcTime);
        when(clock.instant()).thenReturn(instant);
        when(clock.getZone()).thenReturn(ZoneId.of(UTC_ZONE));
    }

    private void setClientLimit(BigDecimal limitSum) {
        var request = new SetLimitRequestDto(ACCOUNT_NUMBER, ExpenseCategory.PRODUCT, limitSum);
        restClient.post()
                .uri(URI_CREATE_LIMIT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private TransactionResponseDto sendTransaction(BigDecimal sumKzt, String datetimeStr) {
        var request = new TransactionRequestDto(ACCOUNT_NUMBER, COUNTERPARTY, CURRENCY_CODE_KZT, sumKzt,
                ExpenseCategory.PRODUCT, ZonedDateTime.parse(datetimeStr));
        return restClient.post()
                .uri(URI_CREATE_TRANSACTION)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TransactionResponseDto.class);
    }
}