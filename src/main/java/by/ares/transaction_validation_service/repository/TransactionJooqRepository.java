package by.ares.transaction_validation_service.repository;

import by.ares.transaction_validation_service.dto.ExceededTransactionResponseDto;
import by.ares.transaction_validation_service.model.ExpenseCategory;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.DatePart;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static by.ares.transaction_validation_service.util.TransactionValidationServiceConst.*;
import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
public class TransactionJooqRepository {

    private final DSLContext dsl;

    public void acquireClientLock(String accountNumber, ExpenseCategory category) {
        dsl.insertInto(table(TABLE_LIMIT_LOCKS))
                .set(field(COL_ACCOUNT_NUMBER), accountNumber)
                .set(field(COL_EXPENSE_CATEGORY), category.name())
                .set(field(COL_LOCKED_AT), currentOffsetDateTime())
                .onConflict(field(COL_ACCOUNT_NUMBER), field(COL_EXPENSE_CATEGORY))
                .doUpdate()
                .set(field(COL_LOCKED_AT), currentOffsetDateTime())
                .execute();
    }

    public List<ExceededTransactionResponseDto> findExceededTransactionsByAccountNumber(String accountNumber) {
        var transaction = table(TABLE_TRANSACTIONS).as(ALIAS_TRANSACTION);
        var limit = table(TABLE_EXPENSE_LIMITS).as(ALIAS_LIMIT);
        var txDatetimeField = field(name(ALIAS_TRANSACTION, COL_DATETIME), OffsetDateTime.class);
        var limitDatetimeField = field(name(ALIAS_LIMIT, COL_LIMIT_DATETIME), OffsetDateTime.class);
        return dsl.select(
                        field(name(ALIAS_TRANSACTION, COL_ACCOUNT_FROM), String.class),
                        field(name(ALIAS_TRANSACTION, COL_ACCOUNT_TO), String.class),
                        field(name(ALIAS_TRANSACTION, COL_CURRENCY_SHORTNAME), String.class),
                        field(name(ALIAS_TRANSACTION, COL_SUM), BigDecimal.class),
                        field(name(ALIAS_TRANSACTION, COL_EXPENSE_CATEGORY), String.class),
                        txDatetimeField,
                        coalesce(field(name(ALIAS_LIMIT, COL_LIMIT_SUM), BigDecimal.class), DEFAULT_LIMIT)
                                .as(COL_LIMIT_SUM),
                        coalesce(limitDatetimeField, trunc(txDatetimeField, DatePart.MONTH))
                                .as(COL_LIMIT_DATETIME),
                        coalesce(field(name(ALIAS_LIMIT, COL_LIMIT_CURRENCY_SHORTNAME), String.class), DEFAULT_CURRENCY_CODE)
                                .as(COL_LIMIT_CURRENCY_SHORTNAME)
                )
                .from(transaction)
                .leftJoin(limit).on(field(name(ALIAS_TRANSACTION, COL_APPLIED_LIMIT_ID), Long.class)
                        .eq(field(name(ALIAS_LIMIT, COL_ID), Long.class)))
                .where(field(name(ALIAS_TRANSACTION, COL_ACCOUNT_FROM), String.class).eq(accountNumber))
                .and(field(name(ALIAS_TRANSACTION, COL_LIMIT_EXCEEDED), Boolean.class).isTrue())
                .orderBy(txDatetimeField.asc())
                .fetch(r -> new ExceededTransactionResponseDto(
                        r.value1(),
                        r.value2(),
                        r.value3(),
                        r.value4(),
                        ExpenseCategory.fromCode(r.value5()),
                        r.value6() != null ? r.value6().toZonedDateTime() : null,
                        r.value7(),
                        r.value8() != null ? r.value8().toZonedDateTime() : null,
                        r.value9()
                ));
    }
}