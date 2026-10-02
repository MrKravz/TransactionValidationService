package by.ares.transaction_validation_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(
        name = "expense_limits",
        indexes = {
                @Index(name = "idx_limits_acc_cat_dt", columnList = "account_number, expense_category, limit_datetime DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 10)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_category", nullable = false, length = 16)
    private ExpenseCategory expenseCategory;

    @Column(name = "limit_sum", nullable = false, precision = 15, scale = 2)
    private BigDecimal limitSum;

    @Builder.Default
    @Column(name = "limit_currency_shortname", nullable = false, length = 3)
    private String limitCurrencyShortname = "USD";

    @Column(name = "limit_datetime", nullable = false)
    private ZonedDateTime limitDatetime;
}