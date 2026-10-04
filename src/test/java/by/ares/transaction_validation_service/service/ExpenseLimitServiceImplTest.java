package by.ares.transaction_validation_service.service;

import by.ares.transaction_validation_service.dto.ExpenseLimitDto;
import by.ares.transaction_validation_service.dto.SetLimitRequestDto;
import by.ares.transaction_validation_service.mapper.ExpenseLimitMapper;
import by.ares.transaction_validation_service.model.ExpenseCategory;
import by.ares.transaction_validation_service.model.ExpenseLimit;
import by.ares.transaction_validation_service.repository.ExpenseLimitRepository;
import by.ares.transaction_validation_service.service.impl.ExpenseLimitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static by.ares.transaction_validation_service.TestConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseLimitServiceImplTest {

    @Mock
    private ExpenseLimitRepository repository;
    @Mock
    private ExpenseLimitMapper mapper;

    private ExpenseLimitServiceImpl service;
    private Clock fixedClock;

    @Captor
    private ArgumentCaptor<ExpenseLimit> limitCaptor;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse(SYS_TIME_OCT_03), ZoneId.of(UTC_ZONE));
        service = new ExpenseLimitServiceImpl(fixedClock, mapper, repository);
    }

    @Test
    void shouldReturnMappedDtoListWhenLimitsFound() {
        var entity = ExpenseLimit.builder().accountNumber(ACCOUNT_ALT).build();
        var entities = List.of(entity);
        var dto = new ExpenseLimitDto(LIMIT_ID_1, ACCOUNT_ALT, ExpenseCategory.PRODUCT, LIMIT_1000,
                CURRENCY_CODE_USD, ZonedDateTime.now());
        var dtos = List.of(dto);
        when(repository.findAllByAccountNumberOrderByLimitDatetimeDesc(ACCOUNT_ALT)).thenReturn(entities);
        when(mapper.toDtoList(entities)).thenReturn(dtos);
        var result = service.getClientLimits(ACCOUNT_ALT);
        assertEquals(1, result.size());
        assertEquals(dtos, result);
        verify(repository).findAllByAccountNumberOrderByLimitDatetimeDesc(ACCOUNT_ALT);
        verify(mapper).toDtoList(entities);
    }

    @Test
    void shouldReturnEmptyListWhenNoLimitsFound() {
        when(repository.findAllByAccountNumberOrderByLimitDatetimeDesc(ACCOUNT_ALT)).thenReturn(Collections.emptyList());
        when(mapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());
        var result = service.getClientLimits(ACCOUNT_ALT);
        assertTrue(result.isEmpty());
        verify(repository).findAllByAccountNumberOrderByLimitDatetimeDesc(ACCOUNT_ALT);
    }

    @Test
    void shouldCreateLimitWithCorrectParametersAndMskTimezone() {
        var request = new SetLimitRequestDto(ACCOUNT_ALT, ExpenseCategory.SERVICE, LIMIT_1500);
        var savedEntity = ExpenseLimit.builder().id(LIMIT_ID_1).build();
        var expectedDto = new ExpenseLimitDto(LIMIT_ID_1, ACCOUNT_ALT, ExpenseCategory.SERVICE, LIMIT_1500,
                CURRENCY_CODE_USD, ZonedDateTime.now());
        when(repository.save(any(ExpenseLimit.class))).thenReturn(savedEntity);
        when(mapper.toDto(savedEntity)).thenReturn(expectedDto);
        var result = service.createLimit(request);
        verify(repository).save(limitCaptor.capture());
        var capturedLimit = limitCaptor.getValue();
        assertEquals(ACCOUNT_ALT, capturedLimit.getAccountNumber());
        assertEquals(ExpenseCategory.SERVICE, capturedLimit.getExpenseCategory());
        assertEquals(LIMIT_1500, capturedLimit.getLimitSum());
        assertEquals(CURRENCY_CODE_USD, capturedLimit.getLimitCurrencyShortname());
        var expectedMskTime = ZonedDateTime.now(fixedClock).withZoneSameInstant(ZONE_ID);
        assertEquals(expectedMskTime, capturedLimit.getLimitDatetime());
        assertEquals(ZONE_ID, capturedLimit.getLimitDatetime().getZone());
        assertEquals(expectedDto, result);
    }
}