package by.ares.transaction_validation_service.service;

import by.ares.transaction_validation_service.service.impl.RateCachingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;

import static by.ares.transaction_validation_service.TestConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateCachingServiceImplTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RateCachingServiceImpl cachingService;

    private static final String EXPECTED_KEY = "fx:rate:" + PAIR_KZT_USD + ":" + TEST_DATE;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldCacheRateSuccessfully() {
        String jsonRate = "480.00";
        when(objectMapper.writeValueAsString(RATE_480_00)).thenReturn(jsonRate);
        cachingService.cacheRate(PAIR_KZT_USD, TEST_DATE, RATE_480_00);
        verify(objectMapper).writeValueAsString(RATE_480_00);
        verify(valueOperations).set(eq(EXPECTED_KEY), eq(jsonRate), any(Duration.class));
    }

    @Test
    void shouldGetCachedRateSuccessfullyWhenKeyExists() {
        String cachedJson = "480.00";
        when(valueOperations.get(EXPECTED_KEY)).thenReturn(cachedJson);
        when(objectMapper.readValue(cachedJson, BigDecimal.class)).thenReturn(RATE_480_00);
        BigDecimal result = cachingService.getCachedRate(PAIR_KZT_USD, TEST_DATE);
        assertNotNull(result);
        assertEquals(RATE_480_00, result);
        verify(valueOperations).get(EXPECTED_KEY);
        verify(objectMapper).readValue(cachedJson, BigDecimal.class);
    }

    @Test
    void shouldReturnNullWhenKeyDoesNotExist() {
        when(valueOperations.get(EXPECTED_KEY)).thenReturn(null);
        BigDecimal result = cachingService.getCachedRate(PAIR_KZT_USD, TEST_DATE);
        assertNull(result);
        verify(valueOperations).get(EXPECTED_KEY);
        verifyNoInteractions(objectMapper);
    }
}