package by.ares.transaction_validation_service.controller;

import by.ares.transaction_validation_service.dto.ExceededTransactionResponseDto;
import by.ares.transaction_validation_service.dto.TransactionRequestDto;
import by.ares.transaction_validation_service.dto.TransactionResponseDto;
import by.ares.transaction_validation_service.service.TransactionProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TransactionController implements TransactionApi {

    private final TransactionProcessingService transactionProcessingService;

    @Override
    public ResponseEntity<List<ExceededTransactionResponseDto>> getExceededTransactions(String accountFrom) {
        return ResponseEntity.ok(transactionProcessingService.getExceededTransactions(accountFrom));
    }

    @Override
    public ResponseEntity<TransactionResponseDto> processTransaction(TransactionRequestDto transactionRequest) {
        return ResponseEntity.ok(transactionProcessingService.processTransaction(transactionRequest));
    }
}