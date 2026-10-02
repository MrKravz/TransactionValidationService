package by.ares.transaction_validation_service.repository;

import by.ares.transaction_validation_service.model.ExpenseCategory;
import by.ares.transaction_validation_service.model.BankTransaction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Repository
public interface TransactionRepository extends JpaRepository<BankTransaction, Long> {
    BigDecimal sumUsdByAccountAndCategoryAndDates(@NotNull(message = "account_from is required") @Pattern(regexp = "^\\d{10}$", message = "account_from must be exactly 10 digits") String s, @NotNull(message = "expense_category is required") ExpenseCategory expenseCategory, ZonedDateTime startOfMonth, ZonedDateTime txDatetimeMsk);
}
