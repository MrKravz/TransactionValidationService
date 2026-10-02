package by.ares.transaction_validation_service.mapper;

import by.ares.transaction_validation_service.dto.TransactionResponseDto;
import by.ares.transaction_validation_service.model.BankTransaction;
import org.mapstruct.Mapper;

@Mapper
public interface TransactionMapper {
    TransactionResponseDto toResponseDto(BankTransaction transaction);
}
