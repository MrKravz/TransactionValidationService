package by.ares.transaction_validation_service.service.impl;

import by.ares.transaction_validation_service.dto.ExceededTransactionResponseDto;
import by.ares.transaction_validation_service.dto.TransactionRequestDto;
import by.ares.transaction_validation_service.dto.TransactionResponseDto;
import by.ares.transaction_validation_service.repository.TransactionJooqRepository;
import by.ares.transaction_validation_service.service.CurrencyRateService;
import by.ares.transaction_validation_service.service.TransactionProcessingService;
import by.ares.transaction_validation_service.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionProcessingServiceImpl implements TransactionProcessingService {

    private final TransactionJooqRepository transactionJooqRepository;
    private final CurrencyRateService currencyRateService;
    private final TransactionExecutionService transactionExecutionService;

    @Override
    public TransactionResponseDto processTransaction(TransactionRequestDto request) {
        var transactionDatetime = DateTimeUtils.toZone(request.datetime());
        var transactionDate = DateTimeUtils.extractZoneLocalDate(request.datetime());
        var closestRate = currencyRateService.getCloseRate(request.currencyShortname(), transactionDate);
        return transactionExecutionService.executeTransactionWithLock(request, transactionDatetime, closestRate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExceededTransactionResponseDto> getExceededTransactions(String accountNumber) {
        return transactionJooqRepository.findExceededTransactionsByAccountNumber(accountNumber);
    }
}