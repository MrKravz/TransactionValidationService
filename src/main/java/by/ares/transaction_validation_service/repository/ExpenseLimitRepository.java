package by.ares.transaction_validation_service.repository;

import by.ares.transaction_validation_service.model.ExpenseCategory;
import by.ares.transaction_validation_service.model.ExpenseLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseLimitRepository extends JpaRepository<ExpenseLimit, Long> {
    List<ExpenseLimit> findAllByAccountNumberOrderByLimitDatetimeDesc(String accountNumber);

    @Query("""
        SELECT l 
        FROM ExpenseLimit l 
        WHERE l.accountNumber = :accountNumber 
          AND l.expenseCategory = :expenseCategory 
          AND l.limitDatetime <= :txDatetime 
        ORDER BY l.limitDatetime DESC 
        LIMIT 1
    """)
    Optional<ExpenseLimit> findFirstByAccountAndCategoryBeforeDate(@Param("accountNumber") String accountNumber,
                                                                   @Param("expenseCategory") ExpenseCategory expenseCategory,
                                                                   @Param("txDatetime") ZonedDateTime txDatetime);
}