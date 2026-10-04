package by.ares.transaction_validation_service.service.impl;

import by.ares.transaction_validation_service.dto.ExceededTransactionResponseDto;
import by.ares.transaction_validation_service.dto.TransactionRequestDto;
import by.ares.transaction_validation_service.dto.TransactionResponseDto;
import by.ares.transaction_validation_service.mapper.TransactionMapper;
import by.ares.transaction_validation_service.model.BankTransaction;
import by.ares.transaction_validation_service.repository.ExpenseLimitRepository;
import by.ares.transaction_validation_service.repository.TransactionJooqRepository;
import by.ares.transaction_validation_service.repository.TransactionRepository;
import by.ares.transaction_validation_service.service.CurrencyRateService;
import by.ares.transaction_validation_service.service.TransactionProcessingService;
import by.ares.transaction_validation_service.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.List;

import static by.ares.transaction_validation_service.util.TransactionValidationServiceConst.DEFAULT_LIMIT;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionProcessingServiceImpl implements TransactionProcessingService {

    private final TransactionRepository transactionRepository;
    private final ExpenseLimitRepository expenseLimitEntityRepository;
    private final TransactionJooqRepository transactionJooqRepository;
    private final CurrencyRateService currencyRateService;
    private final TransactionMapper transactionMapper;

    @Lazy
    @Autowired
    private TransactionProcessingServiceImpl self;

    @Override
    public TransactionResponseDto processTransaction(TransactionRequestDto request) {
        var transactionDatetime = DateTimeUtils.toZone(request.datetime());
        var transactionDate = DateTimeUtils.extractZoneLocalDate(request.datetime());
        var closestRate = currencyRateService.getCloseRate(request.currencyShortname(), transactionDate);
        return self.executeTransactionWithLock(request, transactionDatetime, closestRate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExceededTransactionResponseDto> getExceededTransactions(String accountNumber) {
        return transactionJooqRepository.findExceededTransactionsByAccountNumber(accountNumber);
    }

    @Transactional
    public TransactionResponseDto executeTransactionWithLock(TransactionRequestDto request, ZonedDateTime transactionDatetime,
            BigDecimal closestRate) {
        var sumUsd = request.sum().divide(closestRate, 2, RoundingMode.HALF_UP);
        var startOfMonth = DateTimeUtils.getZoneStartOfMonth(transactionDatetime);
        transactionJooqRepository.acquireClientLock(request.accountFrom(), request.expenseCategory());
        var effectiveLimit = expenseLimitEntityRepository.findFirstByAccountAndCategoryBeforeDate(request.accountFrom(),
                        request.expenseCategory(), transactionDatetime).orElse(null);
        var limitSum = effectiveLimit != null ? effectiveLimit.getLimitSum() : DEFAULT_LIMIT;
        BigDecimal spentThisMonth = transactionRepository.sumUsdByAccountAndCategoryAndDates(request.accountFrom(),
                request.expenseCategory(), startOfMonth, transactionDatetime);
        if (spentThisMonth == null) {
            spentThisMonth = BigDecimal.ZERO;
        }
        BankTransaction newTransaction = BankTransaction.builder()
                .accountFrom(request.accountFrom())
                .accountTo(request.accountTo())
                .currencyShortname(request.currencyShortname())
                .sum(request.sum())
                .expenseCategory(request.expenseCategory())
                .datetime(transactionDatetime)
                .sumUsd(sumUsd)
                .limitExceeded(isLimitExceeded(spentThisMonth, sumUsd, limitSum))
                .appliedLimit(effectiveLimit)
                .build();
        BankTransaction savedTransaction = transactionRepository.save(newTransaction);
        log.info("Transaction processed ID: {}, Account: {}, Sum USD: {}, Exceeded: {}", savedTransaction.getId(),
                savedTransaction.getAccountFrom(), savedTransaction.getSumUsd(), savedTransaction.isLimitExceeded());
        return transactionMapper.toResponseDto(savedTransaction);
    }

    private boolean isLimitExceeded(BigDecimal spendThisMonth, BigDecimal currentTransactionSumUsd, BigDecimal limitSum) {
        var totalWithCurrent = spendThisMonth.add(currentTransactionSumUsd);
        return totalWithCurrent.compareTo(limitSum) > 0;
    }
}