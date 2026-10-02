package by.ares.transaction_validation_service.mapper;

import by.ares.transaction_validation_service.dto.ExpenseLimitDto;
import by.ares.transaction_validation_service.model.ExpenseLimit;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface ExpenseLimitMapper {
    ExpenseLimitDto toDto(ExpenseLimit expenseLimit);

    List<ExpenseLimitDto> toDtoList(List<ExpenseLimit> limits);
}
