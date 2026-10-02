package by.ares.transaction_validation_service.service.impl;

import by.ares.transaction_validation_service.dto.ExpenseLimitDto;
import by.ares.transaction_validation_service.dto.SetLimitRequestDto;
import by.ares.transaction_validation_service.mapper.ExpenseLimitMapper;
import by.ares.transaction_validation_service.model.ExpenseLimit;
import by.ares.transaction_validation_service.repository.ExpenseLimitRepository;
import by.ares.transaction_validation_service.service.ExpenseLimitService;
import by.ares.transaction_validation_service.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseLimitServiceImpl implements ExpenseLimitService {

    private final ExpenseLimitRepository expenseLimitEntityRepository;
    private final ExpenseLimitMapper expenseLimitMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseLimitDto> getClientLimits(String accountNumber) {
        List<ExpenseLimit> limits = expenseLimitEntityRepository
                .findAllByAccountNumberOrderByLimitDatetimeDesc(accountNumber);

        return expenseLimitMapper.toDtoList(limits);
    }

    @Override
    @Transactional
    public ExpenseLimitDto createLimit(SetLimitRequestDto request) {
        var zonedDateTime = DateTimeUtils.toMskZone(ZonedDateTime.now(clock));
        ExpenseLimit newLimit = ExpenseLimit.builder()
                .accountNumber(request.accountFrom())
                .expenseCategory(request.expenseCategory())
                .limitSum(request.limitSum())
                .limitCurrencyShortname("USD")
                .limitDatetime(zonedDateTime)
                .build();
        ExpenseLimit saved = expenseLimitEntityRepository.save(newLimit);
        log.info("New limit created for account {} [Category: {}, Sum: {} USD, Time: {}]",
                saved.getAccountNumber(), saved.getExpenseCategory(), saved.getLimitSum(), saved.getLimitDatetime());
        return expenseLimitMapper.toDto(saved);
    }
}