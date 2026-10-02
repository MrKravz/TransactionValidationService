package by.ares.transaction_validation_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_trans_acc_cat_dt", columnList = "account_from, expense_category, datetime")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_from", nullable = false, length = 10)
    private String accountFrom;

    @Column(name = "account_to", nullable = false, length = 10)
    private String accountTo;

    @Column(name = "currency_shortname", nullable = false, length = 3)
    private String currencyShortname;

    @Column(name = "sum", nullable = false, precision = 15, scale = 2)
    private BigDecimal sum;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_category", nullable = false, length = 16)
    private ExpenseCategory expenseCategory;

    @Column(name = "datetime", nullable = false)
    private ZonedDateTime datetime;

    @Column(name = "sum_usd", nullable = false, precision = 15, scale = 2)
    private BigDecimal sumUsd;

    @Column(name = "limit_exceeded", nullable = false)
    private boolean limitExceeded;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_limit_id", foreignKey = @ForeignKey(name = "fk_tx_applied_limit"))
    private ExpenseLimit appliedLimit;
}