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
        val dtStart = dateTimeAt(1997, 9, 2, 9)
        val rule = RecurringRule(Frequency.DAILY, interval = 10, count = 5)

        val entries = rule.getNextEntries(dtStart).toList()
        assertIterableEquals(
            listOf(
                dateTimeAt(1997, 9, 2, 9),
                dateTimeAt(1997, 9, 12, 9),
                dateTimeAt(1997, 9, 22, 9),
                dateTimeAt(1997, 10, 2, 9),
                dateTimeAt(1997, 10, 12, 9),
            ), entries
        )
    }

    @Test
    fun testEveryDayInJanuaryForThreeYears() {
        val dtStart = dateTimeAt(1997, 9, 2, 9)
        val rule = RecurringRule(Frequency.DAILY, endDate = dateTimeAt(2000, 1, 31, 14), byMonth = listOf(1))

        val entries = rule.getNextEntries(dtStart).toList()
        for (i in 1..3) {
            for (j in 0..30) {
                val entry = entries[(i - 1) * 31 + j]
                assertEquals(dateTimeAt(1997 + i, 1, j + 1, 9), entry)
            }
        }
    }

    @Test
    fun testWeeklyFor10Occurrences() {
        val dtStart = dateTimeAt(1997, 9, 2, 9)
        val rule = RecurringRule(Frequency.WEEKLY, count = 10)
        val entries = rule.getNextEntries(dtStart).toList()

        assertIterableEquals(
            listOf(
                dateTimeAt(1997, 9, 2, 9),
                dateTimeAt(1997, 9, 9, 9),
                dateTimeAt(1997, 9, 16, 9),
                dateTimeAt(1997, 9, 23, 9),
                dateTimeAt(1997, 9, 30, 9),
                dateTimeAt(1997, 10, 7, 9),
                dateTimeAt(1997, 10, 14, 9),
                dateTimeAt(1997, 10, 21, 9),
                dateTimeAt(1997, 10, 28, 9),
                dateTimeAt(1997, 11, 4, 9)
            ), entries
        )
    }

    @Test
    fun testWeekUntilDecember241997() {
        val dtStart = dateTimeAt(1997, 9, 2, 9)
        val rule = RecurringRule(Frequency.WEEKLY, endDate = dateTimeAt(1997, 12, 24))

        val entries = rule.getNextEntries(dtStart).toList()
        assertEquals(17, entries.size)
        // TODO
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
    fun testWeeklyOnTuesdayAndThursdayForFiveWeeks() {
        val dtStart = dateTimeAt(1997, 9, 2, 9)
        val tuesdayAndThursday = listOf(WeekDayOccurrence(WeekDay.Tuesday), WeekDayOccurrence(WeekDay.Thursday))
        val rules = listOf(
            RecurringRule(
                Frequency.WEEKLY,
                endDate = dateTimeAt(1997, 10, 7),
                weekStart = WeekDay.Sunday,
                byWeekDay = tuesdayAndThursday
            ),
            RecurringRule(
                Frequency.WEEKLY,
                count = 10,
                weekStart = WeekDay.Sunday,
                byWeekDay = tuesdayAndThursday
            )
        )

        for (rule in rules) {
            val entries = rule.getNextEntries(dtStart).toList()
            assertIterableEquals(
                listOf(
                    dateTimeAt(1997, 9, 2, 9),
                    dateTimeAt(1997, 9, 4, 9),
                    dateTimeAt(1997, 9, 9, 9),
                    dateTimeAt(1997, 9, 11, 9),
                    dateTimeAt(1997, 9, 16, 9),
                    dateTimeAt(1997, 9, 18, 9),
                    dateTimeAt(1997, 9, 23, 9),
                    dateTimeAt(1997, 9, 25, 9),
                    dateTimeAt(1997, 9, 30, 9),
                    dateTimeAt(1997, 10, 2, 9),
                ), entries
            )
        }
    }

    @Test
    fun testEveryOtherWeekOnMonWedFriUntilDec241997() {
        val dtStart = dateTimeAt(1997, 9, 1, 9)
        val rule = RecurringRule(
            Frequency.WEEKLY,
            interval = 2,
            endDate = dateTimeAt(1997, 12, 24),
            weekStart = WeekDay.Sunday,
            byWeekDay = listOf(
                WeekDayOccurrence(WeekDay.Monday),
                WeekDayOccurrence(WeekDay.Wednesday),
                WeekDayOccurrence(WeekDay.Friday)
            )
        )

        val entries = rule.getNextEntries(dtStart).toList()
        assertEquals(25, entries.size)
        // TODO
    }

    @Test
    fun testEveryOtherWeekOnTuesdayAndThursdayFor8Occurrences() {
        val dtStart = dateTimeAt(1997, 9, 2, 9)
        val rule = RecurringRule(
            Frequency.WEEKLY,
            interval = 2,
            count = 8,
            weekStart = WeekDay.Sunday,
            byWeekDay = listOf(WeekDayOccurrence(WeekDay.Tuesday), WeekDayOccurrence(WeekDay.Thursday))
        )

        val entries = rule.getNextEntries(dtStart).toList()
        assertIterableEquals(listOf(
            dateTimeAt(1997, 9, 2, 9),
            dateTimeAt(1997, 9, 4, 9),
            dateTimeAt(1997, 9, 16, 9),
            dateTimeAt(1997, 9, 18, 9),
            dateTimeAt(1997, 9, 30, 9),
            dateTimeAt(1997, 10, 2, 9),
            dateTimeAt(1997, 10, 14, 9),
            dateTimeAt(1997, 10, 16, 9),
        ), entries)
    }

    @Test
    fun testMonthlyOnTheFirstFridayFor10Occurrences() {
        val dtStart = dateTimeAt(1997, 9, 2, 9)
        val rule = RecurringRule(Frequency.MONTHLY, count = 10, byWeekDay = listOf(WeekDayOccurrence(WeekDay.Friday, 1)))
        val entries = rule.getNextEntries(dtStart).toList()

        assertIterableEquals(listOf(
            dateTimeAt(1997, 9, 5, 9),
            dateTimeAt(1997, 10, 3, 9),
            dateTimeAt(1997, 11, 7, 9),
            dateTimeAt(1997, 12, 5, 9),
            dateTimeAt(1998, 1, 2, 9),
            dateTimeAt(1998, 2, 6, 9),
            dateTimeAt(1998, 3, 6, 9),
            dateTimeAt(1998, 4, 3, 9),
            dateTimeAt(1998, 5, 1, 9),
            dateTimeAt(1998, 6, 5, 9),
        ), entries)
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
