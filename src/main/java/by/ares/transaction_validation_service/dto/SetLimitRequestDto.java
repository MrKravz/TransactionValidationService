package by.ares.transaction_validation_service.dto;

import by.ares.transaction_validation_service.model.ExpenseCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SetLimitRequestDto(
        @NotNull(message = "account_from is required")
        @Pattern(regexp = "^\\d{10}$", message = "account_from must be exactly 10 digits")
        @JsonProperty("account_from")
        String accountFrom,
        @NotNull(message = "expense_category is required")
        @JsonProperty("expense_category")
        ExpenseCategory expenseCategory,
        @NotNull(message = "limit_sum is required")
        @Positive(message = "limit_sum must be greater than 0")
        @JsonProperty("limit_sum")
        BigDecimal limitSum
) {}
