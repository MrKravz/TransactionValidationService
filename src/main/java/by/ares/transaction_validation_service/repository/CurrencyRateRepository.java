package by.ares.transaction_validation_service.repository;

import by.ares.transaction_validation_service.model.CurrencyRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, Long> {
    Optional<CurrencyRate> findByCurrencyPairAndRateDate(String pair, LocalDate date);

    Optional<CurrencyRate> findTopByCurrencyPairAndRateDateLessThanEqualOrderByRateDateDesc(String pair, LocalDate date);

    Set<LocalDate> findExistingDates(String pair, List<LocalDate> responseDates);
}