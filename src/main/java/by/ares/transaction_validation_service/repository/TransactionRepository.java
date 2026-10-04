package by.ares.transaction_validation_service.repository;

import by.ares.transaction_validation_service.model.BankTransaction;
import by.ares.transaction_validation_service.model.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Repository
public interface TransactionRepository extends JpaRepository<BankTransaction, Long> {

    @Query("""
        SELECT SUM(t.sumUsd) 
        FROM BankTransaction t 
        WHERE t.accountFrom = :accountFrom 
          AND t.expenseCategory = :expenseCategory 
          AND t.datetime >= :startDate 
          AND t.datetime <= :endDate
    """)
    BigDecimal sumUsdByAccountAndCategoryAndDates(
            @Param("accountFrom") String accountFrom,
            @Param("expenseCategory") ExpenseCategory expenseCategory,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate
    );
}