package by.ares.transaction_validation_service.util;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;

public final class TransactionValidationServiceConst {

    public static final ZoneId ZONE = ZoneId.of("Europe/Moscow");
    public static final BigDecimal DEFAULT_LIMIT = new BigDecimal("1000.00");
    public static final String DEFAULT_CURRENCY_CODE = "USD";
    public static final String EXCHANGE_RATE_INTERVAL = "1day";
    public static final String API_TIMEOUT_MESSAGE = "Api timed out";
    public static final String FX_RATE_NOT_FOUND_MESSAGE = "Critical Error: FX API is down and NO fallback rate found" +
            " in DB for ";
    public static final String TABLE_LIMIT_LOCKS = "limit_locks";
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String TABLE_EXPENSE_LIMITS = "expense_limits";
    public static final String ALIAS_TRANSACTION = "transaction";
    public static final String ALIAS_LIMIT = "limit";
    public static final String COL_ACCOUNT_NUMBER = "account_number";
    public static final String COL_EXPENSE_CATEGORY = "expense_category";
    public static final String COL_LOCKED_AT = "locked_at";
    public static final String COL_DATETIME = "datetime";
    public static final String COL_ACCOUNT_FROM = "account_from";
    public static final String COL_ACCOUNT_TO = "account_to";
    public static final String COL_CURRENCY_SHORTNAME = "currency_shortname";
    public static final String COL_SUM = "sum";
    public static final String COL_LIMIT_EXCEEDED = "limit_exceeded";
    public static final String COL_APPLIED_LIMIT_ID = "applied_limit_id";
    public static final String COL_LIMIT_DATETIME = "limit_datetime";
    public static final String COL_LIMIT_SUM = "limit_sum";
    public static final String COL_LIMIT_CURRENCY_SHORTNAME = "limit_currency_shortname";
    public static final String COL_ID = "id";
    public static final String RATE_KEY_PREFIX = "fx:rate:";
    public static final Duration RATE_CACHE_TTL = Duration.ofDays(1);

    private TransactionValidationServiceConst() {}
}