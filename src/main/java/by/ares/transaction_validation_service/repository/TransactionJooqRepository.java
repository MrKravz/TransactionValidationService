package by.ares.transaction_validation_service.repository;

import by.ares.transaction_validation_service.dto.ExceededTransactionResponseDto;
import by.ares.transaction_validation_service.model.ExpenseCategory;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.DatePart;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
public class TransactionJooqRepository {

    private final DSLContext dsl;

    public void acquireClientLock(String accountNumber, ExpenseCategory category) {
        dsl.insertInto(table("limit_locks"))
                .set(field("account_number"), accountNumber)
                .set(field("expense_category"), category.name())
                .set(field("locked_at"), currentOffsetDateTime())
                .onConflict(field("account_number"), field("expense_category"))
                .doUpdate()
                .set(field("locked_at"), currentOffsetDateTime())
                .execute();
    }

    public List<ExceededTransactionResponseDto> findExceededTransactionsByAccountNumber(String accountNumber) {
        var transaction = table("transactions").as("transaction");
        var limit = table("expense_limits").as("limit");
        var txDatetimeField = field(transaction.getName() + ".datetime", ZonedDateTime.class);
        var limitDatetimeField = field(limit.getName() + ".limit_datetime", ZonedDateTime.class);
        return dsl.select(
                        field(transaction.getName() + ".account_from", String.class),
                        field(transaction.getName() + ".account_to", String.class),
                        field(transaction.getName() + ".currency_shortname", String.class),
                        field(transaction.getName() + ".sum", BigDecimal.class),
                        field(transaction.getName() + ".expense_category", String.class),
                        txDatetimeField,
                        coalesce(field(limit.getName() + ".limit_sum", BigDecimal.class), new BigDecimal("1000.00")).as("limit_sum"),
                        coalesce(limitDatetimeField, trunc(txDatetimeField, DatePart.MONTH)).as("limit_datetime"),
                        coalesce(field(limit.getName() + ".limit_currency_shortname", String.class), "USD").as("limit_currency_shortname")
                )
                .from(transaction)
                .leftJoin(limit).on(field(transaction.getName() + ".applied_limit_id", Long.class).eq(field(limit.getName() + ".id", Long.class)))
                .where(field(transaction.getName() + ".account_from", String.class).eq(accountNumber))
                .and(field(transaction.getName() + ".limit_exceeded", Boolean.class).isTrue())
                .orderBy(txDatetimeField.desc())
                .fetch(r -> new ExceededTransactionResponseDto(
                        r.value1(),
                        r.value2(),
                        r.value3(),
                        r.value4(),
                        ExpenseCategory.fromCode(r.value5()),
                        r.value6(),
                        r.value7(),
                        r.value8(),
                        r.value9()
                ));
    }
}