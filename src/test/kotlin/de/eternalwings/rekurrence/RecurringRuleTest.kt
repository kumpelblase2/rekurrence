package de.eternalwings.rekurrence

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZoneId
import java.time.ZonedDateTime

class RecurringRuleTest {
    @Test
    fun testEmptyInterval() {
        val startDay = dateTimeAt(2021, 1, 1)
        val recurringRule = RecurringRule(Frequency.YEARLY, interval = 0)
        assertTrue(recurringRule.getNextEntries(startDay).none())
    }

    @Test
    fun testParse() {
        assertEquals(RecurringRule(Frequency.YEARLY, interval = 2), RecurringRule.fromString("FREQ=YEARLY;INTERVAL=2"))
        assertEquals(
            RecurringRule(Frequency.MONTHLY, count = 2, byMinute = listOf(1, 2)),
            RecurringRule.fromString("FREQ=MONTHLY;COUNT=2;BYMINUTE=1,2")
        )
        assertEquals(
            RecurringRule(Frequency.WEEKLY, byWeekDay = listOf(WeekDayOccurrence(WeekDay.Monday))),
            RecurringRule.fromString("FREQ=WEEKLY;BYDAY=MO")
        )
        assertEquals(
            RecurringRule(Frequency.MONTHLY, byWeekDay = listOf(WeekDayOccurrence(WeekDay.Tuesday, 2))),
            RecurringRule.fromString("FREQ=MONTHLY;BYDAY=2TU")
        )


        assertEquals("FREQ=MONTHLY;COUNT=2;BYMINUTE=1,2", RecurringRule.fromString("FREQ=MONTHLY;COUNT=2;BYMINUTE=1,2").asString())
        assertEquals("FREQ=MONTHLY;BYDAY=2TU", RecurringRule.fromString("FREQ=MONTHLY;BYDAY=2TU").asString())
    }

    @Test
    fun testSimpleInterval() {
        val startingDate = dateTimeAt(2021, 1, 1)
        val recurringRule = RecurringRule(Frequency.DAILY, interval = 2)

        val nextTwoEntries = recurringRule.getNextEntries(startingDate).take(2).toList()
        val firstDate = dateTimeAt(2021, 1, 3)
        val secondDate = dateTimeAt(2021, 1, 5)
        assertEquals(firstDate, nextTwoEntries[0])
        assertEquals(secondDate, nextTwoEntries[1])
    }

    @Test
    fun testSimpleIntervalWeeks() {
        val startingDate = dateTimeAt(2021, 1, 1)
        val recurringRule = RecurringRule(Frequency.WEEKLY)

        val nextTwoEntries = recurringRule.getNextEntries(startingDate).take(2).toList()
        val firstDate = dateTimeAt(2021, 1, 8)
        val secondDate = dateTimeAt(2021, 1, 15)
        assertEquals(firstDate, nextTwoEntries[0])
        assertEquals(secondDate, nextTwoEntries[1])
    }

    @Test
    fun testSimpleIntervalWithCount() {
        val startingDate = dateTimeAt(2021, 1, 1)
        val recurringRule = RecurringRule(Frequency.DAILY, interval = 2, count = 1)

        val nextEntries = recurringRule.getNextEntries(startingDate).toList()
        val firstDate = dateTimeAt(2021, 1, 3)
        assertEquals(firstDate, nextEntries[0])
        assertEquals(1, nextEntries.size)
    }

    @Test
    fun testMultipleHours() {
        val startingDate = dateTimeAt(2021, 1, 1, hour = 6)
        val recurringRule = RecurringRule(Frequency.YEARLY, interval = 2, byHour = listOf(1, 4))

        val nextEntries = recurringRule.getNextEntries(startingDate).take(4).toList()
        assertEquals(dateTimeAt(2023, 1, 1, 1), nextEntries[0])
        assertEquals(dateTimeAt(2023, 1, 1, 4), nextEntries[1])
        assertEquals(dateTimeAt(2025, 1, 1, 1), nextEntries[2])
        assertEquals(dateTimeAt(2025, 1, 1, 4), nextEntries[3])
    }

    @Test
    fun testErrorOnInvalidValues() {
        assertThrows<IllegalArgumentException> { RecurringRule(Frequency.YEARLY, byHour = listOf(24)) }
        assertThrows<IllegalArgumentException> { RecurringRule(Frequency.YEARLY, byMonth = listOf(0)) }
        assertThrows<IllegalArgumentException> { RecurringRule(Frequency.YEARLY, bySetPosition = listOf(367)) }
        assertThrows<IllegalArgumentException> { RecurringRule.fromString("FREQ=YEARLY;BYHOUR=-1") }
        assertThrows<IllegalArgumentException> { RecurringRule.fromString("FREQ=YEARLY;BYHOUR=24") }
        assertThrows<IllegalArgumentException> {
            RecurringRule(
                Frequency.WEEKLY,
                byWeekDay = listOf(WeekDayOccurrence(WeekDay.Tuesday, 2))
            )
        }
    }

    @Test
    fun testYearBoundaries() {
        val startingDate = dateTimeAt(2020, 12, 28, hour = 8)
        val recurringRule = RecurringRule(Frequency.WEEKLY, byHour = listOf(8))
        val next = recurringRule.getNextEntries(startingDate).first()
        assertEquals(dateTimeAt(2021, 1, 4, 8), next)
    }

    @Test
    fun testNextInstanceCanBeWithoutInterval() {
        val startingDate = dateTimeAt(2021, 1, 1)
        val recurringRule = RecurringRule(Frequency.YEARLY, byYearDay = listOf(2))
        val next = recurringRule.getNextEntries(startingDate).first()
        assertEquals(dateTimeAt(2021, 1, 2), next)
    }

    @Test
    fun testNegativeMonthDay() {
        val startingDate = dateTimeAt(2021, 1, 1)
        val recurringRule = RecurringRule(Frequency.MONTHLY, byMonthDay = listOf(-3))
        val (first, second) = recurringRule.getNextEntries(startingDate).take(2).toList()
        assertEquals(dateTimeAt(2021, 1, 29), first)
        assertEquals(dateTimeAt(2021, 2, 26), second)
    }

    companion object {
        private val UTC = ZoneId.of("UTC")

        private fun dateTimeAt(
            year: Int,
            month: Int,
            day: Int,
            hour: Int = 0,
            minute: Int = 0,
            second: Int = 0
        ): ZonedDateTime {
            return ZonedDateTime.of(year, month, day, hour, minute, second, 0, UTC)
        }
    }
}
