package by.ares.transaction_validation_service.service;

import by.ares.transaction_validation_service.dto.ExpenseLimitDto;
import by.ares.transaction_validation_service.dto.SetLimitRequestDto;

import java.util.List;

public interface ExpenseLimitService {
    List<ExpenseLimitDto> getClientLimits(String accountNumber);

    ExpenseLimitDto createLimit(SetLimitRequestDto request);
}
