package by.ares.transaction_validation_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "currency_rates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_pair_date", columnNames = {"currency_pair", "rate_date"})
        },
        indexes = {
                @Index(name = "idx_rates_pair_date", columnList = "currency_pair, rate_date DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "currency_pair", nullable = false, length = 7)
    private String currencyPair;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(name = "close_rate", nullable = false, precision = 18, scale = 6)
    private BigDecimal closeRate;
}