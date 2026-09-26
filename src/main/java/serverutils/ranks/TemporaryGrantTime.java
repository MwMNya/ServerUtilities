package serverutils.ranks;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

public final class TemporaryGrantTime {

    private static final Pattern DURATION = Pattern.compile("^([1-9][0-9]*)([smhdw])$", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    public static final class Range {

        private final long validFrom;
        private final long validUntil;

        public Range(long validFrom, long validUntil) {
            if (validFrom >= validUntil) throw new IllegalArgumentException("Grant end must be after its start");
            this.validFrom = validFrom;
            this.validUntil = validUntil;
        }

        public long validFrom() {
            return validFrom;
        }

        public long validUntil() {
            return validUntil;
        }

        public boolean isActive(long now) {
            return now >= validFrom && now < validUntil;
        }
    }

    private TemporaryGrantTime() {}

    @Nullable
    public static Range parse(String input, long now, ZoneId zone) {
        String value = input.trim().toLowerCase(Locale.ROOT);

        if (value.equals("month")) {
            ZonedDateTime start = Instant.ofEpochMilli(now).atZone(zone);
            return new Range(start.toInstant().toEpochMilli(), start.plusMonths(1).toInstant().toEpochMilli());
        }

        int separator = value.indexOf("..");
        if (separator > 0 && separator == value.lastIndexOf("..")) {
            try {
                LocalDate startDate = LocalDate.parse(value.substring(0, separator));
                LocalDate endDate = LocalDate.parse(value.substring(separator + 2));
                long start = startDate.atStartOfDay(zone).toInstant().toEpochMilli();
                long end = endDate.atStartOfDay(zone).toInstant().toEpochMilli();
                return end > start && end > now ? new Range(start, end) : null;
            } catch (DateTimeException ignored) {
                return null;
            }
        }

        Matcher matcher = DURATION.matcher(value);
        if (!matcher.matches()) return null;

        try {
            long amount = Long.parseLong(matcher.group(1));
            long unitMillis = switch (Character.toLowerCase(matcher.group(2).charAt(0))) {
                case 's' -> 1_000L;
                case 'm' -> 60_000L;
                case 'h' -> 3_600_000L;
                case 'd' -> 86_400_000L;
                case 'w' -> 604_800_000L;
                default -> throw new IllegalStateException();
            };
            long end = Math.addExact(now, Math.multiplyExact(amount, unitMillis));
            return new Range(now, end);
        } catch (ArithmeticException ignored) {
            return null;
        }
    }

    /**
     * Parses a grant while preserving any unexpired time already owned by the target. Relative grants are appended to
     * the existing end time; explicit date ranges remain absolute.
     */
    @Nullable
    public static Range parseForGrant(String input, long now, ZoneId zone, @Nullable Range existing) {
        String value = input.trim().toLowerCase(Locale.ROOT);

        if (existing != null && existing.validUntil() > now && isRelative(value)) {
            Range extension = parse(value, existing.validUntil(), zone);
            return extension == null ? null : new Range(existing.validFrom(), extension.validUntil());
        }

        return parse(value, now, zone);
    }

    private static boolean isRelative(String value) {
        return value.equals("month") || DURATION.matcher(value).matches();
    }

    public static long timeUntil(long timestamp, long now) {
        return timestamp <= now ? 0L : timestamp - now;
    }

    public static String format(long timestamp, ZoneId zone) {
        return DISPLAY.format(Instant.ofEpochMilli(timestamp).atZone(zone));
    }
}
