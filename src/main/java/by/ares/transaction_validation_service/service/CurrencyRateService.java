package by.ares.transaction_validation_service.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CurrencyRateService {
    BigDecimal getCloseRate(String currency, LocalDate date);
}