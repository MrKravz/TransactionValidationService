package by.ares.transaction_validation_service.util;

import java.math.BigDecimal;
import java.time.ZoneId;

public final class TransactionValidationServiceConst {
    public static final ZoneId ZONE = ZoneId.of("Europe/Moscow");
    public static final BigDecimal DEFAULT_LIMIT = new BigDecimal("1000.00");

    private TransactionValidationServiceConst() {}
}
