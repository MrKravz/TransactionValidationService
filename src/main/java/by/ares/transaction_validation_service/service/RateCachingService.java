package by.ares.transaction_validation_service.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RateCachingService {
    void cacheRate(String pair, LocalDate date, BigDecimal rate);

    BigDecimal getCachedRate(String pair, LocalDate date);
}
