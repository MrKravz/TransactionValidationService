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
import java.time.ZonedDateTime;
import java.util.List;

import static by.ares.transaction_validation_service.util.TransactionValidationServiceConst.DEFAULT_CURRENCY_CODE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseLimitServiceImpl implements ExpenseLimitService {

    private final Clock clock;
    private final ExpenseLimitMapper expenseLimitMapper;
    private final ExpenseLimitRepository expenseLimitEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseLimitDto> getClientLimits(String accountNumber) {
        var limits = expenseLimitEntityRepository
                .findAllByAccountNumberOrderByLimitDatetimeDesc(accountNumber);
        return expenseLimitMapper.toDtoList(limits);
    }

    @Override
    @Transactional
    public ExpenseLimitDto createLimit(SetLimitRequestDto request) {
        var zonedDateTime = DateTimeUtils.toZone(ZonedDateTime.now(clock));
        var newLimit = ExpenseLimit.builder()
                .accountNumber(request.accountFrom())
                .expenseCategory(request.expenseCategory())
                .limitSum(request.limitSum())
                .limitCurrencyShortname(DEFAULT_CURRENCY_CODE)
                .limitDatetime(zonedDateTime)
                .build();
        var savedLimit = expenseLimitEntityRepository.save(newLimit);
        log.info("New limit created for account {} [Category: {}, Sum: {} USD, Time: {}]",
                savedLimit.getAccountNumber(), savedLimit.getExpenseCategory(),
                savedLimit.getLimitSum(), savedLimit.getLimitDatetime());
        return expenseLimitMapper.toDto(savedLimit);
    }
}