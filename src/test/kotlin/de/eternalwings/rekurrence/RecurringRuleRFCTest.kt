package de.eternalwings.rekurrence

import de.eternalwings.rekurrence.Frequency.DAILY
import de.eternalwings.rekurrence.Frequency.MONTHLY
import de.eternalwings.rekurrence.Frequency.YEARLY
import de.eternalwings.rekurrence.WeekDay.Monday
import de.eternalwings.rekurrence.WeekDay.Tuesday
import de.eternalwings.rekurrence.support.testForever
import de.eternalwings.rekurrence.support.testOccurrences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
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

        for (i in 0 until summerTimeDays) {
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
            assertTimeDifferenceOf(Duration.ofDays(2), previous, next)
            previous = next
        }
    }

    @Test
    fun testEvery10Days5Occurrences() {
        testOccurrences {
            start = dateTimeAt(1997, 9, 2, 9)
            rule = RecurringRule(DAILY, interval = 10, count = 5)
            result = listOf(
                dateTimeAt(1997, 9, 2, 9),
                dateTimeAt(1997, 9, 12, 9),
                dateTimeAt(1997, 9, 22, 9),
                dateTimeAt(1997, 10, 2, 9),
                dateTimeAt(1997, 10, 12, 9),
            )
        }
    }

    @Test
    fun testEveryDayInJanuaryForThreeYears() {
        val dtStart = dateTimeAt(1997, 9, 1, 9)
        val rule = RecurringRule(Frequency.DAILY, endDate = dateTimeAt(2000, 1, 31, 14), byMonth = listOf(1))

        val entries = rule.getNextEntries(dtStart).toList()
        val expectedList = mutableListOf<ZonedDateTime>()
        for (i in 1..3) {
            for (j in 0..30) {
                expectedList.add(dateTimeAt(1997 + i, 1, j + 1, 9))
            }
        }

        assertIterableEquals(expectedList, entries)
    }

    @Test
    fun testEveyOtherWeekForever() {
        val dtStart = dateTimeAt(1997, 9, 2, 9)
        val rule = RecurringRule(Frequency.WEEKLY, interval = 2, weekStart = WeekDay.Sunday)

        val entryIterator = rule.getNextEntries(dtStart).iterator()
        val first = entryIterator.next()
        assertEquals(dateTimeAt(1997, 9, 2, 9), first)
        var previous = first
        for (i in 0..BASICALLY_FOREVER) {
            val next = entryIterator.next()
            assertTimeDifferenceOf(Duration.ofDays(14), previous, next)
            previous = next
        }
    }

    @Test
    fun testEveryTuesdayEveryOtherMonth() {
        testForever {
            start = dateTimeAt(1997, 9, 2, 9)
            rule = RecurringRule(
                MONTHLY,
                interval = 2,
                byWeekDay = listOf(WeekDayOccurrence(Tuesday))
            )
            result = generateSequence(start) {
                val next = it.plusWeeks(1)
                if (next.month == it.month) {
                    next
                } else {
                    next.plusMonths(1).with(TemporalAdjusters.dayOfWeekInMonth(1, TUESDAY))
                }
            }.asIterable()
        }
    }

    @Test
    fun testEvery20thMondayOfTheYearForever() {
        testForever {
            start = dateTimeAt(1997, 5, 19, 9)
            rule = RecurringRule(
                YEARLY,
                byWeekDay = listOf(WeekDayOccurrence(Monday, 20))
            )
            result = generateSequence(start) {
                it.plusYears(1).with(TemporalAdjusters.firstDayOfYear()).with(TemporalAdjusters.nextOrSame(MONDAY))
                    .plusWeeks(19)
            }.asIterable()
        }
    }

    @Test
    fun testMondayOfWeekNumber20Forever() {
        testForever {
            start = dateTimeAt(1997, 5, 12, 9)
            rule = RecurringRule(
                YEARLY,
                byWeekNr = listOf(20),
                byWeekDay = listOf(WeekDayOccurrence(Monday))
            )
            result = generateSequence(start) {
                val nextYear = it.plusYears(1)
                nextYear.with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, 20).with(TemporalAdjusters.previousOrSame(MONDAY))
            }.asIterable()
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

        private fun assertTimeDifferenceOf(duration: Duration, start: ZonedDateTime, end: ZonedDateTime) {
            val previousOffset = start.toOffsetDateTime().offset.totalSeconds.absoluteValue
            val nextOffset = end.toOffsetDateTime().offset.totalSeconds.absoluteValue
            assertEquals(duration, Duration.between(start, end).plusSeconds((previousOffset - nextOffset).toLong())) {
                "Time between $start and $end was not $duration."
            }
        }
    }
}
