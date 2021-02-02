package de.eternalwings.rekurrence

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

data class RecurringRule(
    val frequency: Frequency,
    val endDate: ZonedDateTime? = null,
    val count: Int? = null,
    val interval: Int = DEFAULT_INTERVAL,
    val bySecond: List<Int> = emptyList(),
    val byMinute: List<Int> = emptyList(),
    val byHour: List<Int> = emptyList(),
    val byWeekDay: List<WeekDayOccurrence> = emptyList(),
    val byMonthDay: List<Int> = emptyList(),
    val byYearDay: List<Int> = emptyList(),
    val byWeekNr: List<Int> = emptyList(),
    val byMonth: List<Int> = emptyList(),
    val bySetPosition: List<Int> = emptyList(),
    val weekStart: WeekDay = DEFAULT_WEEK_START
) {
    init {
        if (endDate != null && count != null) {
            throw IllegalArgumentException("Cannot set both 'endDate' and 'count' in recurring schedule.")
        }
        checkInRange(bySecond, 0, 60) {
            throw IllegalArgumentException("Second recurring rule contains invalid value '$it'.")
        }
        checkInRange(byMinute, 0, 60) {
            throw IllegalArgumentException("Minute recurring rule contains invalid value '$it'.")
        }
        checkInRange(byHour, 0, 23) {
            throw IllegalArgumentException("Hour recurring rule contains invalid value '$it'.")
        }
        checkInRange(byMonthDay, 1, 31, allowNegative = true) {
            throw IllegalArgumentException("Month Day recurring rule contains invalid value '$it'.")
        }
        checkInRange(byYearDay, 1, 366, allowNegative = true) {
            throw IllegalArgumentException("Year Day recurring rule contains invalid value '$it'.")
        }
        checkInRange(byMonth, 1, 12) {
            throw IllegalArgumentException("Month Day recurring rule contains invalid value '$it'.")
        }
        checkInRange(bySetPosition, 1, 366, allowNegative = true) {
            throw IllegalArgumentException("Month Day recurring rule contains invalid value '$it'.")
        }
        checkInRange(byWeekDay.map { it.occurrence ?: 1 }, 1, 53, allowNegative = true) {
            throw IllegalArgumentException("Week Day recurring rule contains invalid value '$it'.")
        }
        byWeekDay.firstOrNull { it.occurrence != null }?.let {
            if (frequency != Frequency.MONTHLY && frequency != Frequency.YEARLY) {
                throw IllegalArgumentException("Week Day recurring rule has number specified '$it' with $frequency frequency. That is illegal.")
            }
            if (frequency == Frequency.YEARLY && byWeekNr.isNotEmpty()) {
                throw IllegalArgumentException("Week Day recurring rule has number specified '$it' with yearly frequency with week number is specified. That is illegal.")
            }
        }
    }

    fun getNextEntries(startingInstance: ZonedDateTime): Sequence<ZonedDateTime> {
        return OccurrenceGenerator.generateFrom(this, startingInstance)
    }

    fun asString(): String {
        return buildString {
            append("FREQ=").append(frequency.name)
            endDate?.let { append(";UNTIL=").append(DATETIME_FORMAT.format(it)) }
            count?.let { append(";COUNT=").append(it) }
            if (interval != DEFAULT_INTERVAL) {
                append(";INTERVAL=").append(interval)
            }
            if (bySecond.isNotEmpty()) {
                append(";BYSECOND=")
                append(bySecond.joinToString(","))
            }
            if (byMinute.isNotEmpty()) {
                append(";BYMINUTE=")
                append(byMinute.joinToString(","))
            }
            if (byHour.isNotEmpty()) {
                append(";BYHOUR=")
                append(byHour.joinToString(","))
            }
            if (byWeekDay.isNotEmpty()) {
                append(";BYDAY=")
                append(byWeekDay.joinToString(",") { (it.occurrence?.toString() ?: "") + it.weekDay.shortName })
            }
            if (byMonthDay.isNotEmpty()) {
                append(";BYMONTHDAY=")
                append(byMonthDay.joinToString(","))
            }
            if (byYearDay.isNotEmpty()) {
                append(";BYYEARDAY=")
                append(byYearDay.joinToString(","))
            }
            if (byWeekNr.isNotEmpty()) {
                append(";BYWEEKNO=")
                append(byWeekNr.joinToString(","))
            }
            if (byMonth.isNotEmpty()) {
                append(";BYMONTH=")
                append(byMonth.joinToString(","))
            }
            if (bySetPosition.isNotEmpty()) {
                append(";BYSETPOS=")
                append(bySetPosition.joinToString(","))
            }
            if (weekStart != WeekDay.Monday) {
                append(";WKST=")
                append(weekStart.shortName)
            }
        }
    }

    class Builder {
        var frequency: Frequency = DEFAULT_FREQUENCY
        var endDate: ZonedDateTime? = null
        var count: Int? = null
        var interval: Int = DEFAULT_INTERVAL
        var bySecond: List<Int> = emptyList()
        var byMinute: List<Int> = emptyList()
        var byHour: List<Int> = emptyList()
        var byWeekDay: List<WeekDayOccurrence> = emptyList()
        var byMonthDay: List<Int> = emptyList()
        var byYearDay: List<Int> = emptyList()
        var byWeekNr: List<Int> = emptyList()
        var byMonth: List<Int> = emptyList()
        var bySetPosition: List<Int> = emptyList()
        var weekStart: WeekDay = DEFAULT_WEEK_START

        fun build(): RecurringRule {
            return RecurringRule(
                frequency,
                endDate,
                count,
                interval,
                bySecond,
                byMinute,
                byHour,
                byWeekDay,
                byMonthDay,
                byYearDay,
                byWeekNr,
                byMonth,
                bySetPosition,
                weekStart
            )
        }
    }

    companion object {
        private val DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
        private val WEEKDAY_OCCURRENCE_PATTERN = "^([+-]?\\d{1,2})?(MO|TU|WE|TH|FR|SA|SU)$".toRegex()
        private const val DEFAULT_INTERVAL = 1
        private val DEFAULT_WEEK_START = WeekDay.Monday
        private val DEFAULT_FREQUENCY = Frequency.WEEKLY

        private fun checkInRange(
            values: Collection<Int>,
            start: Int,
            end: Int,
            allowNegative: Boolean = false,
            errorFunction: (Int) -> Nothing
        ) {
            for (value in values) {
                val absVal = if (allowNegative) abs(value) else value
                if (absVal < start || absVal > end) {
                    errorFunction(value)
                }
            }
        }

        fun fromString(toParse: String): RecurringRule {
            val builder = Builder()
            toParse.split(";").forEach { entry ->
                val (decl, value) = entry.split("=")
                when (decl) {
                    "FREQ" -> builder.frequency = Frequency.valueOf(value.toUpperCase())
                    "UNTIL" -> builder.endDate = ZonedDateTime.parse(value, DATETIME_FORMAT)
                    "COUNT" -> builder.count = value.toInt()
                    "INTERVAL" -> builder.interval = value.toInt()
                    "BYSECOND" -> builder.bySecond = value.asIntList()
                    "BYMINUTE" -> builder.byMinute = value.asIntList()
                    "BYHOUR" -> builder.byHour = value.asIntList()
                    "BYDAY" -> builder.byWeekDay = value.split(",").map { it.toWeekDayOccurrence() }
                    "BYMONTHDAY" -> builder.byMonthDay = value.asIntList()
                    "BYYEARDAY" -> builder.byYearDay = value.asIntList()
                    "BYWEEKNO" -> builder.byWeekNr = value.asIntList()
                    "BYMONTH" -> builder.byMonth = value.asIntList()
                    "BYSETPOS" -> builder.bySetPosition = value.asIntList()
                    "WKST" -> builder.weekStart = WeekDay.fromShortName(value)!!
                }
            }

            return builder.build()
        }

        private fun String.asIntList(): List<Int> {
            return this.split(",").map { it.toInt() }
        }

        private fun String.toWeekDayOccurrence(): WeekDayOccurrence {
            val matchedPattern = WEEKDAY_OCCURRENCE_PATTERN.matchEntire(this)
                ?: throw IllegalArgumentException("Weekday Occurrence pattern does not match specification.")
            val (_, occurrenceString, weekdayName) = matchedPattern.groupValues
            val occurrence = if (occurrenceString.isEmpty()) null else occurrenceString.toInt()
            val weekDay =
                WeekDay.fromShortName(weekdayName)!! // we can guarantee that this is correct because of the regex pattern used

            return WeekDayOccurrence(weekDay, occurrence)
        }
    }
}
