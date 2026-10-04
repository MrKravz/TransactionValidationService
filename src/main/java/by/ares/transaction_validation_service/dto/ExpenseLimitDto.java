package by.ares.transaction_validation_service.dto;

import by.ares.transaction_validation_service.model.ExpenseCategory;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record ExpenseLimitDto(@JsonProperty("id") Long id,
                              @JsonProperty("account_number") String accountNumber,
                              @JsonProperty("expense_category") ExpenseCategory expenseCategory,
                              @JsonProperty("limit_sum") BigDecimal limitSum,
                              @JsonProperty("limit_currency_shortname") String limitCurrencyShortname,
                              @JsonProperty("limit_datetime") ZonedDateTime limitDatetime) {
}
