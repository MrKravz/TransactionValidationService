package by.ares.transaction_validation_service.service.impl;

import by.ares.transaction_validation_service.service.RateCachingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

import static by.ares.transaction_validation_service.util.TransactionValidationServiceConst.RATE_CACHE_TTL;
import static by.ares.transaction_validation_service.util.TransactionValidationServiceConst.RATE_KEY_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateCachingServiceImpl implements RateCachingService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void cacheRate(String pair, LocalDate date, BigDecimal rate) {
        if (rate == null) {
            return;
        }
        var key = buildRateKey(pair, date);
        redisTemplate.opsForValue().set(key, rate.toPlainString(), RATE_CACHE_TTL);
        log.debug("Successfully cached rate for key: {}", key);
    }

    @Override
    public BigDecimal getCachedRate(String pair, LocalDate date) {
        String key = buildRateKey(pair, date);
        String val = redisTemplate.opsForValue().get(key);
        if (val == null || val.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            log.warn("Invalid cached BigDecimal value for key {}: {}", key, val);
            return null;
        }
    }

    private String buildRateKey(String pair, LocalDate date) {
        return String.format("%s%s:%s", RATE_KEY_PREFIX, pair, date);
    }
}