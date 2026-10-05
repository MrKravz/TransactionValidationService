package by.ares.transaction_validation_service.service;

import by.ares.transaction_validation_service.dto.TransactionRequestDto;
import by.ares.transaction_validation_service.repository.TransactionJooqRepository;
import by.ares.transaction_validation_service.service.impl.TransactionExecutionService;
import by.ares.transaction_validation_service.service.impl.TransactionProcessingServiceImpl;
import by.ares.transaction_validation_service.util.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;

import static by.ares.transaction_validation_service.TestConstants.*;
import static by.ares.transaction_validation_service.model.ExpenseCategory.PRODUCT;
import static by.ares.transaction_validation_service.util.TransactionValidationServiceConst.ZONE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionProcessingServiceImplTest {

    @Mock
    private TransactionJooqRepository jooqRepository;
    @Mock
    private CurrencyRateService currencyRateService;
    @Mock
    private TransactionExecutionService transactionExecutionService;

    @InjectMocks
    private TransactionProcessingServiceImpl service;

    @Test
    void shouldDelegateProcessTransactionToExecutionService() {
        var txDate = ZonedDateTime.now(ZONE);
        var request = new TransactionRequestDto(ACCOUNT_ALT, COUNTERPARTY, CURRENCY_CODE_KZT, SUM_48000_00,
                PRODUCT, txDate);
        when(currencyRateService.getCloseRate(eq(CURRENCY_CODE_KZT), any())).thenReturn(RATE_480_00);

        service.processTransaction(request);

        verify(transactionExecutionService).executeTransactionWithLock(eq(request), eq(DateTimeUtils.toZone(txDate)),
                eq(RATE_480_00)
        );
    }

    @Test
    void shouldDelegateExceededTransactionsFetchToJooqRepository() {
        service.getExceededTransactions(ACCOUNT_ALT);
        verify(jooqRepository).findExceededTransactionsByAccountNumber(ACCOUNT_ALT);
    }
}