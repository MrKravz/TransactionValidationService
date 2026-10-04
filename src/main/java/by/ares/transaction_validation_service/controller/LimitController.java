package by.ares.transaction_validation_service.controller;

import by.ares.transaction_validation_service.dto.ExpenseLimitDto;
import by.ares.transaction_validation_service.dto.SetLimitRequestDto;
import by.ares.transaction_validation_service.service.ExpenseLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class LimitController implements LimitApi {

    private final ExpenseLimitService expenseLimitService;

    @Override
    public ResponseEntity<List<ExpenseLimitDto>> getClientLimits(String accountFrom) {
        return ResponseEntity.ok(expenseLimitService.getClientLimits(accountFrom));
    }

    @Override
    public ResponseEntity<ExpenseLimitDto> setClientLimit(SetLimitRequestDto setLimitRequestDto) {
        return ResponseEntity.ok(expenseLimitService.createLimit(setLimitRequestDto));
    }
}