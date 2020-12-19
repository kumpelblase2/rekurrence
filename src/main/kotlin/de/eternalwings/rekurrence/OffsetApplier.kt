package de.eternalwings.rekurrence

import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

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
        // TODO this does not handle year changes in e.g. a daily rule when only a specific month is set.
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
