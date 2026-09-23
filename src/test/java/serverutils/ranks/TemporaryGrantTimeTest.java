package serverutils.ranks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.Test;

public class TemporaryGrantTimeTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Test
    public void fixedDaysAreExactTwentyFourHourPeriods() {
        long now = LocalDateTime.of(2026, 1, 20, 12, 34).atZone(ZONE).toInstant().toEpochMilli();
        TemporaryGrantTime.Range range = TemporaryGrantTime.parse("30d", now, ZONE);
        assertNotNull(range);
        assertEquals(now, range.validFrom());
        assertEquals(30L * 24L * 60L * 60L * 1000L, range.validUntil() - range.validFrom());
    }

    @Test
    public void monthRunsFromGrantTimeToSameTimeNextMonth() {
        long now = LocalDateTime.of(2026, 9, 24, 12, 0).atZone(ZONE).toInstant().toEpochMilli();
        TemporaryGrantTime.Range range = TemporaryGrantTime.parse("month", now, ZONE);
        assertNotNull(range);
        assertEquals(now, range.validFrom());
        assertEquals(LocalDateTime.of(2026, 10, 24, 12, 0).atZone(ZONE).toInstant().toEpochMilli(), range.validUntil());
        assertTrue(range.isActive(now));
    }

    @Test
    public void monthClampsToLastDayWhenNextMonthIsShorter() {
        long now = LocalDateTime.of(2026, 1, 31, 8, 30).atZone(ZONE).toInstant().toEpochMilli();
        TemporaryGrantTime.Range range = TemporaryGrantTime.parse("month", now, ZONE);
        assertNotNull(range);
        assertEquals(LocalDateTime.of(2026, 2, 28, 8, 30).atZone(ZONE).toInstant().toEpochMilli(), range.validUntil());
    }

    @Test
    public void explicitRangeIsStartInclusiveAndEndExclusive() {
        long now = LocalDateTime.of(2026, 9, 24, 12, 0).atZone(ZONE).toInstant().toEpochMilli();
        TemporaryGrantTime.Range range = TemporaryGrantTime.parse("2026-09-01..2026-10-01", now, ZONE);
        assertNotNull(range);
        assertTrue(range.isActive(range.validFrom()));
        assertFalse(range.isActive(range.validUntil()));
    }

    @Test
    public void rejectsInvalidAndExpiredRanges() {
        long now = LocalDateTime.of(2026, 9, 24, 12, 0).atZone(ZONE).toInstant().toEpochMilli();
        assertNull(TemporaryGrantTime.parse("thirty-days", now, ZONE));
        assertNull(TemporaryGrantTime.parse("2026-10-01..2026-09-01", now, ZONE));
        assertNull(TemporaryGrantTime.parse("2025-09-01..2025-10-01", now, ZONE));
    }
}
