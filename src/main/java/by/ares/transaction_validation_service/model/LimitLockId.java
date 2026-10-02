package by.ares.transaction_validation_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LimitLockId implements Serializable {

    @Column(name = "account_number", length = 10)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_category", length = 16)
    private ExpenseCategory expenseCategory;
}