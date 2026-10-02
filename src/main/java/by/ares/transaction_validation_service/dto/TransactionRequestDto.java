package by.ares.transaction_validation_service.dto;

import by.ares.transaction_validation_service.model.ExpenseCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record TransactionRequestDto(
        @NotNull(message = "account_from is required")
        @Pattern(regexp = "^\\d{10}$", message = "account_from must be exactly 10 digits")
        @JsonProperty("account_from")
        String accountFrom,

        @NotNull(message = "account_to is required")
        @Pattern(regexp = "^\\d{10}$", message = "account_to must be exactly 10 digits")
        @JsonProperty("account_to")
        String accountTo,

        @NotNull(message = "currency_shortname is required")
        @Size(min = 3, max = 3, message = "currency_shortname must be a 3-letter ISO code")
        @JsonProperty("currency_shortname")
        String currencyShortname,

        @NotNull(message = "sum is required")
        @Positive(message = "sum must be greater than 0")
        @JsonProperty("sum")
        BigDecimal sum,

        @NotNull(message = "expense_category is required")
        @JsonProperty("expense_category")
        ExpenseCategory expenseCategory,

        @NotNull(message = "datetime is required")
        @JsonProperty("datetime")
        ZonedDateTime datetime
) {}