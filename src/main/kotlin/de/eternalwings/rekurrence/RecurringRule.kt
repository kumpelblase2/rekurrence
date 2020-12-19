package de.eternalwings.rekurrence

import java.time.DayOfWeek
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import kotlin.math.abs
import kotlin.math.ceil

// RFC 5545 section-3.3.10
enum class Frequency {
    SECONDLY,
    MINUTELY,
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class WeekDay(val shortName: String) {
    Monday("MO"),
    Tuesday("TU"),
    Wednesday("WE"),
    Thursday("TH"),
    Friday("FR"),
    Saturday("SA"),
    Sunday("SU");

    companion object {
        fun fromShortName(name: String): WeekDay? {
            val uppercaseName = name.toUpperCase()
            return values().find { it.shortName == uppercaseName }
        }
    }
}

data class WeekDayOccurrence(val weekDay: WeekDay, val occurrence: Int? = null)

typealias DirectOffset = (ZonedDateTime) -> ZonedDateTime

data class OffsetApplier(
    private val staticOffset: (ZonedDateTime, Long) -> ZonedDateTime,
    private val specificMonth: Int? = null,
    private val specificMonthDay: Int? = null,
    private val specificYearDay: Int? = null,
    private val specificDay: WeekDayOccurrence? = null,
    private val specificHour: Int? = null,
    private val specificMinute: Int? = null,
    private val specificSecond: Int? = null,
    private val specificWeekNumber: Int? = null,
    private val setPosOffset: Int? = null
) {
    fun applyTo(date: ZonedDateTime, iteration: Long): ZonedDateTime {
        var changed = staticOffset(date, iteration + (setPosOffset ?: 0))

        specificMonth?.let { month ->
            changed = changed.withMonth(month)
        }

        specificWeekNumber?.let { weekNo ->
            changed = changed.with(WeekFields.ISO.weekOfYear(), weekNo.toLong()) // TODO this needs week start info!
        }

        specificYearDay?.let { day ->
            changed = changed.withDayOfYear(day)
        }

        specificMonthDay?.let { day ->
            changed = if (day < 0) {
                val monthLength = changed.toLocalDate().lengthOfMonth()
                changed.withDayOfMonth(monthLength + day + 1) // Need to add +1 since -1 = last day == monthLength
            } else {
                changed.withDayOfMonth(day)
            }
        }

        specificDay?.let { day ->
            val adjuster = TemporalAdjusters.nextOrSame(DayOfWeek.valueOf(day.weekDay.name.toUpperCase()))
            changed = changed.with(adjuster)
            day.occurrence?.let {
                changed = changed.plusWeeks(it.toLong())
            }
        }

        specificHour?.let { hour ->
            changed = changed.withHour(hour)
        }

        specificMinute?.let { minute ->
            changed = changed.withMinute(minute)
        }

        specificSecond?.let { second ->
            changed = changed.withSecond(second)
        }

        return changed
    }
}

class IntervalContainer(rule: RecurringRule) {
    private var iterationIndex = 0
    private val offsets: List<OffsetApplier>

    init {
        val staticOffset: (ZonedDateTime, Long) -> ZonedDateTime = when (rule.frequency) {
            Frequency.SECONDLY -> ({ date, amount -> date.plusSeconds(rule.interval * amount) })
            Frequency.MINUTELY -> ({ date, amount -> date.plusMinutes(rule.interval * amount) })
            Frequency.HOURLY -> ({ date, amount -> date.plusHours(rule.interval * amount) })
            Frequency.DAILY -> ({ date, amount -> date.plusDays(rule.interval * amount) })
            Frequency.WEEKLY -> ({ date, amount -> date.plusWeeks(rule.interval * amount) })
            Frequency.MONTHLY -> ({ date, amount -> date.plusMonths(rule.interval * amount) })
            Frequency.YEARLY -> ({ date, amount -> date.plusYears(rule.interval * amount) })
        }
        var currentList = listOf(OffsetApplier(staticOffset))

        // TODO better explanation
        // Duration(1, Years)
        // Month 1, 2, 3, 4, 5, 6, ...
        // Day 1, 2, 3, 4, ...
        //    |
        //    v
        // Duration(1, Years) + SetMonth(1)
        // Duration(1, Years) + SetMonth(2)
        // Duration(1, Years) + SetMonth(3)

        if (rule.byMonth.isNotEmpty()) {
            currentList = rule.byMonth.flatMap { specified ->
                currentList.map { currentApplier ->
                    currentApplier.copy(specificMonth = specified)
                }
            }
        }

        if (rule.byWeekNr.isNotEmpty()) {
            currentList = rule.byWeekNr.flatMap { specified ->
                currentList.map { currentApplier ->
                    currentApplier.copy(specificWeekNumber = specified)
                }
            }
        }

        if (rule.byYearDay.isNotEmpty()) {
            currentList = rule.byYearDay.flatMap { specified ->
                currentList.map { currentApplier ->
                    currentApplier.copy(specificYearDay = specified)
                }
            }
        }

        if (rule.byMonthDay.isNotEmpty()) {
            currentList = rule.byMonthDay.flatMap { specified ->
                currentList.map { currentApplier ->
                    currentApplier.copy(specificMonthDay = specified)
                }
            }
        }

        if (rule.byWeekDay.isNotEmpty()) {
            currentList = rule.byWeekDay.flatMap { specified ->
                currentList.map { currentApplier ->
                    currentApplier.copy(specificDay = specified)
                }
            }
        }

        if (rule.byHour.isNotEmpty()) {
            currentList = rule.byHour.flatMap { specified ->
                currentList.map { currentApplier ->
                    currentApplier.copy(specificHour = specified)
                }
            }
        }

        if (rule.byMinute.isNotEmpty()) {
            currentList = rule.byMinute.flatMap { specified ->
                currentList.map { currentApplier ->
                    currentApplier.copy(specificMinute = specified)
                }
            }
        }

        if (rule.bySecond.isNotEmpty()) {
            currentList = rule.bySecond.flatMap { specified ->
                currentList.map { currentApplier ->
                    currentApplier.copy(specificSecond = specified)
                }
            }
        }

        if (rule.bySetPosition.isNotEmpty()) {
            currentList = rule.bySetPosition.flatMap { specified ->
                currentList.map { currentApplier ->
                    currentApplier.copy(setPosOffset = specified)
                }
            }
        }


        offsets = currentList
    }

    fun nextInterval(): Pair<DirectOffset, Int> {
        val currentIndex = iterationIndex
        val offsetApplier: DirectOffset = { date ->
            offsets[iterationIndex % offsets.size].applyTo(date, ceil(currentIndex / offsets.size.toDouble()).toLong())
        }
        iterationIndex += 1
        return offsetApplier to currentIndex
    }
}

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
        if (interval == 0) {
            return emptySequence()
        }

        val interval = IntervalContainer(this)
        var remainingCount = count ?: -1

        return generateSequence {
            var (generatedOffset, currentCount) = interval.nextInterval()

            var next = generatedOffset(startingInstance)

            // We generally start with 0 to check if the next date is within the current interval
            // e.g. if we say yearly on the 5th and it's the 4th, the next one should be _this_ year
            // and not the year after
            if (currentCount == 0) {
                while (next < startingInstance) {
                    val (newGeneratedOffset, _) = interval.nextInterval()
                    next = newGeneratedOffset(startingInstance)
                }
            }

            // Check if we reached the amount of counted recurrences
            if (remainingCount == 0) {
                return@generateSequence null
            }
            //            count?.let { specifiedCount ->
            //                if (currentCount > specifiedCount) {
            //                    return@generateSequence null
            //                }
            //            }

            // Make sure we don't pass the end date
            if (endDate != null) {
                if (next > endDate) {
                    return@generateSequence null
                }
            }

            remainingCount -= 1
            return@generateSequence next
        }
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
                    "UNTIL" -> builder.endDate = DATETIME_FORMAT.parse(value) as ZonedDateTime
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
                    "WKST" -> builder.weekStart = WeekDay.valueOf(value)
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
