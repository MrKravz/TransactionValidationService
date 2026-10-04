package by.ares.transaction_validation_service.repository;

import by.ares.transaction_validation_service.model.CurrencyRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, Long> {
    Optional<CurrencyRate> findByCurrencyPairAndRateDate(String pair, LocalDate date);

    Optional<CurrencyRate> findTopByCurrencyPairAndRateDateLessThanEqualOrderByRateDateDesc(String pair, LocalDate date);

    @Query("""
        SELECT c.rateDate
        FROM CurrencyRate c
        WHERE c.currencyPair = :pair
          AND c.rateDate IN :dates
    """)
    Set<LocalDate> findExistingDates(@Param("pair") String pair, @Param("dates") List<LocalDate> responseDates);
}