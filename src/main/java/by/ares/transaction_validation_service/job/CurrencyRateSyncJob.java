package by.ares.transaction_validation_service.job;

import by.ares.transaction_validation_service.service.CurrencySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrencyRateSyncJob {

    private final CurrencySyncService syncService;

    private static final List<String> SUPPORTED_PAIRS = List.of("KZT/USD", "RUB/USD");

    @Scheduled(cron = "0 0 2 * * ?", zone = "Europe/Moscow")
    public void syncDailyRates() {
        log.info("Starting daily currency rates synchronization job...");
        for (var pair : SUPPORTED_PAIRS) {
            try {
                syncService.syncRates(pair);
                log.info("Successfully synchronized rates for {}", pair);
            } catch (Exception e) {
                log.error("Failed to synchronize rates for {} during scheduled job. Reason: {}", pair, e.getMessage());
            }
        }
        log.info("Finished daily currency rates synchronization job.");
    }
}