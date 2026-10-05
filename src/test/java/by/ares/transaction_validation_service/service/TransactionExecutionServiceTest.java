package by.ares.transaction_validation_service.service;

import by.ares.transaction_validation_service.dto.TransactionRequestDto;
import by.ares.transaction_validation_service.mapper.TransactionMapper;
import by.ares.transaction_validation_service.model.BankTransaction;
import by.ares.transaction_validation_service.model.ExpenseCategory;
import by.ares.transaction_validation_service.model.ExpenseLimit;
import by.ares.transaction_validation_service.repository.ExpenseLimitRepository;
import by.ares.transaction_validation_service.repository.TransactionJooqRepository;
import by.ares.transaction_validation_service.repository.TransactionRepository;
import by.ares.transaction_validation_service.service.impl.TransactionExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.Optional;

import static by.ares.transaction_validation_service.TestConstants.*;
import static by.ares.transaction_validation_service.util.TransactionValidationServiceConst.ZONE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionExecutionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ExpenseLimitRepository limitRepository;
    @Mock
    private TransactionJooqRepository jooqRepository;
    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionExecutionService service;

    @Captor
    private ArgumentCaptor<BankTransaction> txCaptor;

    @ParameterizedTest(name = "Spend = {0}, Operation = {1}, Limit = {2} -> Exceeded: {3}")
    @CsvSource({
            "500.00,  100.00, 1000.00, false",
            "900.00,  100.00, 1000.00, false",
            "900.00,  100.01, 1000.00, true",
            "1500.00, 50.00,  1000.00, true",
            "0.00,    100.00, 1000.00, false",
            "0.00,    1000.00,1000.00, false",
            "0.00,    1000.01,1000.00, true"
    })
    void shouldCalculateLimitExceededFlagCorrectly(String spentStr, String txSumStr, String limitStr,
                                                   boolean expectedExceeded) {
        var spentThisMonth = new BigDecimal(spentStr);
        var currentTxSumUsd = new BigDecimal(txSumStr);
        var limitSum = new BigDecimal(limitStr);
        var txDateMsk = ZonedDateTime.now(ZONE);
        var request = new TransactionRequestDto(ACCOUNT_ALT, COUNTERPARTY, CURRENCY_CODE_USD, currentTxSumUsd,
                ExpenseCategory.PRODUCT, txDateMsk);
        var effectiveLimit = ExpenseLimit.builder().limitSum(limitSum).build();

        when(limitRepository.findFirstByAccountAndCategoryBeforeDate(request.accountFrom(), request.expenseCategory(),
                txDateMsk)).thenReturn(Optional.of(effectiveLimit));
        when(transactionRepository.sumUsdByAccountAndCategoryAndDates(eq(request.accountFrom()),
                eq(request.expenseCategory()), any(ZonedDateTime.class), eq(txDateMsk))).thenReturn(spentThisMonth);

        var savedTxMock = BankTransaction.builder().id(TX_ID_1).build();
        when(transactionRepository.save(any(BankTransaction.class))).thenReturn(savedTxMock);

        service.executeTransactionWithLock(request, txDateMsk, BigDecimal.ONE);

        verify(jooqRepository).acquireClientLock(ACCOUNT_ALT, ExpenseCategory.PRODUCT);
        verify(transactionRepository).save(txCaptor.capture());
        var savedTx = txCaptor.getValue();
        assertEquals(expectedExceeded, savedTx.isLimitExceeded());
        assertEquals(currentTxSumUsd.setScale(2, RoundingMode.HALF_UP), savedTx.getSumUsd());
        assertEquals(effectiveLimit, savedTx.getAppliedLimit());
    }

    @Test
    void shouldApplyDefaultLimitWhenNoCustomLimitExists() {
        var txDate = ZonedDateTime.now(ZONE);
        var request = new TransactionRequestDto(ACCOUNT_ALT, COUNTERPARTY, CURRENCY_CODE_KZT, SUM_48000_00,
                ExpenseCategory.PRODUCT, txDate);

        when(limitRepository.findFirstByAccountAndCategoryBeforeDate(any(), any(), any())).thenReturn(Optional.empty());
        when(transactionRepository.sumUsdByAccountAndCategoryAndDates(any(), any(), any(), any())).thenReturn(null);

        var savedTxMock = BankTransaction.builder().id(TX_ID_2).build();
        when(transactionRepository.save(any(BankTransaction.class))).thenReturn(savedTxMock);

        service.executeTransactionWithLock(request, txDate, RATE_480_00);

        verify(transactionRepository).save(txCaptor.capture());
        var savedTx = txCaptor.getValue();
        assertEquals(SUM_100_00, savedTx.getSumUsd());
        assertFalse(savedTx.isLimitExceeded());
        assertNull(savedTx.getAppliedLimit());
    }

    @Test
    void shouldUseUpdatedLimitWhenLimitChangedMidMonth() {
        var txDate = ZonedDateTime.now(ZONE);
        var request = new TransactionRequestDto(ACCOUNT_ALT, COUNTERPARTY, CURRENCY_CODE_USD, SUM_100_00,
                ExpenseCategory.PRODUCT, txDate);
        var newLimit = ExpenseLimit.builder()
                .limitSum(LIMIT_2000)
                .build();
        when(limitRepository.findFirstByAccountAndCategoryBeforeDate(request.accountFrom(), request.expenseCategory(),
                txDate)).thenReturn(Optional.of(newLimit));
        when(transactionRepository.sumUsdByAccountAndCategoryAndDates(any(), any(), any(), any())).thenReturn(SUM_1500_00);

        var savedTxMock = BankTransaction.builder().id(TX_ID_3).build();
        when(transactionRepository.save(any(BankTransaction.class))).thenReturn(savedTxMock);

        service.executeTransactionWithLock(request, txDate, BigDecimal.ONE);

        verify(transactionRepository).save(txCaptor.capture());
        assertFalse(txCaptor.getValue().isLimitExceeded());
    }

    @Test
    void shouldResetSpentAmountOnNewMonthTransition() {
        var txDate = ZonedDateTime.now(ZONE).withDayOfMonth(1).withHour(0).withMinute(1);
        var request = new TransactionRequestDto(ACCOUNT_ALT, COUNTERPARTY, CURRENCY_CODE_USD, SUM_500_00,
                ExpenseCategory.PRODUCT, txDate);
        var limit = ExpenseLimit.builder().limitSum(LIMIT_1000).build();
        when(limitRepository.findFirstByAccountAndCategoryBeforeDate(any(), any(), eq(txDate)))
                .thenReturn(Optional.of(limit));
        when(transactionRepository.sumUsdByAccountAndCategoryAndDates(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        var savedTxMock = BankTransaction.builder().id(TX_ID_4).build();
        when(transactionRepository.save(any(BankTransaction.class))).thenReturn(savedTxMock);

        service.executeTransactionWithLock(request, txDate, BigDecimal.ONE);

        verify(transactionRepository).save(txCaptor.capture());
        assertFalse(txCaptor.getValue().isLimitExceeded());
    }
}
