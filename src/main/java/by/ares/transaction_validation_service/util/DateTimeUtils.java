package by.ares.transaction_validation_service.util;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import static by.ares.transaction_validation_service.util.TransactionValidationServiceConst.ZONE;

public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    public static ZonedDateTime toMskZone(ZonedDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.withZoneSameInstant(ZONE);
    }

    public static LocalDate extractMskLocalDate(ZonedDateTime dateTime) {
        return toMskZone(dateTime).toLocalDate();
    }

    public static ZonedDateTime getStartOfMonthMsk(ZonedDateTime dateTime) {
        return toMskZone(dateTime)
                .with(TemporalAdjusters.firstDayOfMonth())
                .truncatedTo(ChronoUnit.DAYS);
    }
}