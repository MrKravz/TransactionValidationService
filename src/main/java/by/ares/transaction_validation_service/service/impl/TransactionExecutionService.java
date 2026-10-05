package by.ares.transaction_validation_service.service.impl;

import by.ares.transaction_validation_service.dto.TransactionRequestDto;
import by.ares.transaction_validation_service.dto.TransactionResponseDto;
import by.ares.transaction_validation_service.mapper.TransactionMapper;
import by.ares.transaction_validation_service.model.BankTransaction;
import by.ares.transaction_validation_service.repository.ExpenseLimitRepository;
import by.ares.transaction_validation_service.repository.TransactionJooqRepository;
import by.ares.transaction_validation_service.repository.TransactionRepository;
import by.ares.transaction_validation_service.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;

import static by.ares.transaction_validation_service.util.TransactionValidationServiceConst.DEFAULT_LIMIT;

/**
 * Dedicated transactional helper service to execute payment processing under
 * pessimistic locking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionExecutionService {

    private final TransactionRepository transactionRepository;
    private final ExpenseLimitRepository expenseLimitEntityRepository;
    private final TransactionJooqRepository transactionJooqRepository;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponseDto executeTransactionWithLock(TransactionRequestDto request,
            ZonedDateTime transactionDatetime,
            BigDecimal closestRate) {

        transactionJooqRepository.acquireClientLock(request.accountFrom(), request.expenseCategory());

        // 2. Compute USD equivalent
        var sumUsd = request.sum().divide(closestRate, 2, RoundingMode.HALF_UP);
        var startOfMonth = DateTimeUtils.getZoneStartOfMonth(transactionDatetime);

        // 3. Retrieve effective active limit and accumulated monthly spending under pessimistic lock
        var effectiveLimit = expenseLimitEntityRepository.findFirstByAccountAndCategoryBeforeDate(
                request.accountFrom(), request.expenseCategory(), transactionDatetime
        ).orElse(null);

        var limitSum = effectiveLimit != null ? effectiveLimit.getLimitSum() : DEFAULT_LIMIT;

        BigDecimal spentThisMonth = transactionRepository.sumUsdByAccountAndCategoryAndDates(
                request.accountFrom(), request.expenseCategory(), startOfMonth, transactionDatetime
        );
        if (spentThisMonth == null) {
            spentThisMonth = BigDecimal.ZERO;
        }

        boolean limitExceeded = spentThisMonth.add(sumUsd).compareTo(limitSum) > 0;

        BankTransaction newTransaction = BankTransaction.builder()
                .accountFrom(request.accountFrom())
                .accountTo(request.accountTo())
                .currencyShortname(request.currencyShortname())
                .sum(request.sum())
                .expenseCategory(request.expenseCategory())
                .datetime(transactionDatetime)
                .sumUsd(sumUsd)
                .limitExceeded(limitExceeded)
                .appliedLimit(effectiveLimit)
                .build();

        BankTransaction savedTransaction = transactionRepository.save(newTransaction);
        log.info("Transaction processed ID: {}, Account: {}, Sum USD: {}, Exceeded: {}",
                savedTransaction.getId(), savedTransaction.getAccountFrom(),
                savedTransaction.getSumUsd(), savedTransaction.isLimitExceeded());

        return transactionMapper.toResponseDto(savedTransaction);
    }
}
