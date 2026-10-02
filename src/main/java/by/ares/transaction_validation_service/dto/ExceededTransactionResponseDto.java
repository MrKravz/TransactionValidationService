package by.ares.transaction_validation_service.dto;

import by.ares.transaction_validation_service.model.ExpenseCategory;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record ExceededTransactionResponseDto(
        @JsonProperty("account_from")
        String accountFrom,
        @JsonProperty("account_to")
        String accountTo,
        @JsonProperty("currency_shortname")
        String currencyShortname,
        @JsonProperty("sum")
        BigDecimal sum,
        @JsonProperty("expense_category")
        ExpenseCategory expenseCategory,
        @JsonProperty("datetime")
        ZonedDateTime datetime,
        @JsonProperty("limit_sum")
        BigDecimal limitSum,
        @JsonProperty("limit_datetime")
        ZonedDateTime limitDatetime,
        @JsonProperty("limit_currency_shortname")
        String limitCurrencyShortname
) {}
