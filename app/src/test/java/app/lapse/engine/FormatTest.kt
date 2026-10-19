package app.lapse.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FormatTest {
    @Test
    fun durationsReadAsAReport() {
        assertEquals("0m", formatDuration(0))
        assertEquals("45s", formatDuration(45_000))
        assertEquals("6m", formatDuration(6 * 60_000L))
        assertEquals("2h", formatDuration(2 * 3_600_000L))
        assertEquals("1h 26m", formatDuration(86 * 60_000L))
    }

    @Test
    fun pluralHandlesOne() {
        assertEquals("1 reel", plural(1, "reel", "reels"))
        assertEquals("2 reels", plural(2, "reel", "reels"))
    }

    @Test
    fun historyOmitsTheYearForTheCurrentYear() {
        val today = LocalDate.of(2026, 10, 19)
        assertEquals("Mon, Oct 19", formatHistoryDate(today, today))
        assertEquals("Wed, Dec 31, 2025", formatHistoryDate(LocalDate.of(2025, 12, 31), today))
    }
}
