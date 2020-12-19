package de.eternalwings.rekurrence

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.absoluteValue

class RecurringRuleRFCTest {
    @Test
    fun testDailyFor10Occurrences() {
        val dtStart = dateTimeAt(1997, 9, 2, 9)
        val rule = RecurringRule(Frequency.DAILY, count = 10)
        val entries = rule.getNextEntries(dtStart).toList()
        assertEquals(10, entries.size)
        for (i in 2..11) {
            assertEquals(dateTimeAt(1997, 9, i, 9), entries[i - 2])
        }
    }

    @Test
    fun testDailyUntilDec24_1997() {
        val dtStart = dateTimeAt(1997, 9, 2, 9)
        val rule = RecurringRule(Frequency.DAILY, endDate = dateTimeAt(1997, 12, 24))
        val entries = rule.getNextEntries(dtStart).toList()

        val summerTimeDays = 29 /* September 2-30 */ + 25 /* October 1-25 */
        val standardTimeDays = 6 /* October 26-31 */ + 30 /* November 1-30 */ + 23 /* December 1-23 */

        assertEquals(dateTimeAt(1997, 12, 23, 9), entries.last())
        assertEquals(summerTimeDays + standardTimeDays, entries.size)

        for(i in 0 until summerTimeDays) {
            val entry = entries[i]
            assertTrue(entry.zone.rules.isDaylightSavings(entry.toInstant())) {
                "Should be daylight savings time on $entry, but isn't."
            }
        }

        for (i in summerTimeDays until (summerTimeDays + standardTimeDays)) {
            val entry = entries[i]
            assertFalse(entry.zone.rules.isDaylightSavings(entry.toInstant())) {
                "Should be standard time on $entry, but isn't."
            }
        }
    }

    @Test
    fun testEveryOtherDayForever() {
        val dtStart = dateTimeAt(1997, 9, 2, 9)
        val rule = RecurringRule(Frequency.DAILY, interval = 2)

        val sequence = rule.getNextEntries(dtStart).iterator()
        val first = sequence.next()
        assertEquals(dateTimeAt(1997, 9, 2, 9), first)
        var previous = first
        for (i in 0..BASICALLY_FOREVER) {
            val next = sequence.next()
            val previousOffset = previous.toOffsetDateTime().offset.totalSeconds.absoluteValue
            val nextOffset = next.toOffsetDateTime().offset.totalSeconds.absoluteValue
            assertEquals(Duration.ofDays(2), Duration.between(previous, next).plusSeconds((previousOffset - nextOffset).toLong())) {
                "Time between $previous and $next was not two days."
            }
            previous = next
        }
    }

    @Test
    fun testEvery10Days5Occurrences() {
        val dtStart = dateTimeAt(1997, 9, 2, 9)
        val rule = RecurringRule(Frequency.DAILY, interval = 10, count = 5)

        val entries = rule.getNextEntries(dtStart).toList()
        assertIterableEquals(listOf(
            dateTimeAt(1997, 9, 2, 9),
            dateTimeAt(1997, 9, 12, 9),
            dateTimeAt(1997, 9, 22, 9),
            dateTimeAt(1997, 10, 2, 9),
            dateTimeAt(1997, 10, 12, 9),
        ), entries)
    }

    @Test
    fun testEveryDayInJanuarForThreeYears() {
        val dtStart = dateTimeAt(1997, 9, 2, 9)
        val rule = RecurringRule(Frequency.DAILY, endDate = dateTimeAt(2000, 1, 31, 14), byMonth = listOf(1))

        val entries = rule.getNextEntries(dtStart).toList()
        for(i in 1..3) {
            for (j in 0..30) {
                val entry = entries[(i - 1) * 31 + j]
                assertEquals(dateTimeAt(1997 + i, 1, j + 1, 9), entry)
            }
        }
    }

    companion object {
        private val NEW_YORK = ZoneId.of("America/New_York")
        private const val BASICALLY_FOREVER = 500000

        private fun dateTimeAt(
            year: Int,
            month: Int,
            day: Int,
            hour: Int = 0,
            minute: Int = 0,
            second: Int = 0
        ): ZonedDateTime {
            return ZonedDateTime.of(year, month, day, hour, minute, second, 0, NEW_YORK)
        }
    }
}
