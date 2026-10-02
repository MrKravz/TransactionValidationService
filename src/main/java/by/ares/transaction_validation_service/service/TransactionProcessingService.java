package by.ares.transaction_validation_service.service;

import by.ares.transaction_validation_service.dto.ExceededTransactionResponseDto;
import by.ares.transaction_validation_service.dto.TransactionRequestDto;
import by.ares.transaction_validation_service.dto.TransactionResponseDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TransactionProcessingService {
    TransactionResponseDto processTransaction(TransactionRequestDto request);

    @Transactional(readOnly = true)
    List<ExceededTransactionResponseDto> getExceededTransactions(String accountNumber);
}