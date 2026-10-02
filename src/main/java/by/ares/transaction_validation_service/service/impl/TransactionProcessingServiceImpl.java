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

    private final TransactionRepository transactionEntityRepository;
    private final ExpenseLimitRepository expenseLimitEntityRepository;
    private final TransactionJooqRepository transactionJooqRepository;
    private final CurrencyRateService currencyRateService;
    private final TransactionMapper transactionMapper;

    @Lazy
    @Autowired
    private TransactionProcessingServiceImpl self;

    @Override
    public TransactionResponseDto processTransaction(TransactionRequestDto request) {
        var txDatetimeMsk = DateTimeUtils.toMskZone(request.datetime());
        var txDateMsk = DateTimeUtils.extractMskLocalDate(request.datetime());
        var closestRate = currencyRateService.getCloseRate(request.currencyShortname(), txDateMsk);
        return self.executeTransactionWithLock(request, txDatetimeMsk, closestRate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExceededTransactionResponseDto> getExceededTransactions(String accountNumber) {
        return transactionJooqRepository.findExceededTransactionsByAccountNumber(accountNumber);
    }

    @Transactional
    public TransactionResponseDto executeTransactionWithLock(
            TransactionRequestDto request,
            ZonedDateTime txDatetimeMsk,
            BigDecimal closestRate) {
        var sumUsd = request.sum()
                .multiply(closestRate)
                .setScale(2, RoundingMode.HALF_UP);
        var startOfMonth = DateTimeUtils.getStartOfMonthMsk(txDatetimeMsk);
        transactionJooqRepository.acquireClientLock(request.accountFrom(), request.expenseCategory());
        var effectiveLimit = expenseLimitEntityRepository
                .findFirstByAccountAndCategoryBeforeDate(
                        request.accountFrom(),
                        request.expenseCategory(),
                        txDatetimeMsk
                )
                .orElse(null);
        var limitSum = effectiveLimit != null ? effectiveLimit.getLimitSum() : DEFAULT_LIMIT;
        BigDecimal spentThisMonth = transactionEntityRepository.sumUsdByAccountAndCategoryAndDates(
                request.accountFrom(),
                request.expenseCategory(),
                startOfMonth,
                txDatetimeMsk
        );
        if (spentThisMonth == null) {
            spentThisMonth = BigDecimal.ZERO;
        }
        BankTransaction transaction = BankTransaction.builder()
                .accountFrom(request.accountFrom())
                .accountTo(request.accountTo())
                .currencyShortname(request.currencyShortname())
                .sum(request.sum())
                .expenseCategory(request.expenseCategory())
                .datetime(txDatetimeMsk)
                .sumUsd(sumUsd)
                .limitExceeded(isLimitExceeded(spentThisMonth, sumUsd, limitSum))
                .appliedLimit(effectiveLimit)
                .build();
        BankTransaction savedTx = transactionEntityRepository.save(transaction);
        log.info("Transaction processed ID: {}, Account: {}, Sum USD: {}, Exceeded: {}",
                savedTx.getId(), savedTx.getAccountFrom(), savedTx.getSumUsd(), savedTx.isLimitExceeded());
        return transactionMapper.toResponseDto(savedTx);
    }

    private boolean isLimitExceeded(BigDecimal spentThisMonth, BigDecimal currentTxSumUsd, BigDecimal limitSum) {
        BigDecimal totalWithCurrent = spentThisMonth.add(currentTxSumUsd);
        return totalWithCurrent.compareTo(limitSum) > 0;
    }
}