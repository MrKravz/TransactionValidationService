package by.ares.transaction_validation_service.service.impl;

import by.ares.transaction_validation_service.model.CurrencyRate;
import by.ares.transaction_validation_service.repository.CurrencyRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyRateStorageService {

    private final CurrencyRateRepository currencyRateRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCurrencyRates(Set<CurrencyRate> currencyRates, String pair) {
        if (!currencyRates.isEmpty()) {
            try {
                currencyRateRepository.saveAll(currencyRates);
                log.info("Saved {} new rates for {}", currencyRates.size(), pair);
            } catch (DataIntegrityViolationException e) {
                log.warn("Some rates for {} were already inserted concurrently by another process", pair);
            }
        }
    }
}
