package by.ares.transaction_validation_service.repository;

import by.ares.transaction_validation_service.model.ExpenseCategory;
import by.ares.transaction_validation_service.model.ExpenseLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseLimitRepository extends JpaRepository<ExpenseLimit, Long> {
    List<ExpenseLimit> findAllByAccountNumberOrderByLimitDatetimeDesc(String accountNumber);

    @Query("SELECT e FROM ExpenseLimitEntity e " +
            "WHERE e.accountNumber = :accountNumber " +
            "  AND e.expenseCategory = :category " +
            "  AND e.limitDatetime <= :limitDatetime " +
            "ORDER BY e.limitDatetime DESC")
    Optional<ExpenseLimit> findFirstByAccountAndCategoryBeforeDate(String s, ExpenseCategory expenseCategory, ZonedDateTime txDatetimeMsk);
}