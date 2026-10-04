package by.ares.transaction_validation_service.util;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import static by.ares.transaction_validation_service.util.TransactionValidationServiceConst.ZONE;

public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    public static ZonedDateTime toZone(@NotNull ZonedDateTime dateTime) {
        return dateTime.withZoneSameInstant(ZONE);
    }

    public static LocalDate extractZoneLocalDate(ZonedDateTime dateTime) {
        return toZone(dateTime).toLocalDate();
    }

    public static ZonedDateTime getZoneStartOfMonth(ZonedDateTime dateTime) {
        return toZone(dateTime)
                .with(TemporalAdjusters.firstDayOfMonth())
                .truncatedTo(ChronoUnit.DAYS);
    }
}