package de.eternalwings.rekurrence

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

internal interface Filter
internal interface Transformer

sealed class OccurrenceTransformer {
    abstract fun transform(currentDateTime: List<ZonedDateTime>): List<ZonedDateTime>

    abstract class SimpleValueTransformer<T>(
        private val values: List<T>,
        private val transformer: (ZonedDateTime, T) -> ZonedDateTime
    ) : OccurrenceTransformer(), Transformer {
        override fun transform(currentDateTime: List<ZonedDateTime>): List<ZonedDateTime> {
            return currentDateTime.flatMap { dateTime ->
                values.mapNotNull { value ->
                    try {
                        transformer(dateTime, value)
                    } catch (ex: DateTimeException) {
                        null
                    }
                }
            }
        }
    }

    abstract class SimpleValueFilter<T>(private val values: List<T>, private val filteredValueProvider: (ZonedDateTime) -> T) :
        OccurrenceTransformer(), Filter {
        override fun transform(currentDateTime: List<ZonedDateTime>): List<ZonedDateTime> {
            return currentDateTime.filter {
                val value = filteredValueProvider(it)
                values.contains(value)
            }
        }
    }

    class BySecondTransformer(values: List<Int>) : SimpleValueTransformer<Int>(values, { date, second ->
        date.withSecond(second)
    })

    class BySecondFilter(values: List<Int>) : SimpleValueFilter<Int>(values, { it.second })

    class ByMinuteTransformer(values: List<Int>) : SimpleValueTransformer<Int>(values, { date, minute ->
        date.withMinute(minute)
    })

    class ByMinuteFilter(values: List<Int>) : SimpleValueFilter<Int>(values, { it.minute })

    class ByHourTransformer(values: List<Int>) : SimpleValueTransformer<Int>(values, { date, hour ->
        date.withHour(hour)
    })

    class ByHourFilter(values: List<Int>) : SimpleValueFilter<Int>(values, { it.hour })

    class ByMonthDayTransformer(values: List<Int>) : SimpleValueTransformer<Int>(values, { date, day ->
        if (day < 0) {
            val daysInMonth = date.toLocalDate().lengthOfMonth() // TODO is this always correct?
            date.withDayOfMonth(daysInMonth + day + 1)
        } else {
            date.withDayOfMonth(day)
        }
    })

    class ByYearDayTransformer(values: List<Int>) : SimpleValueTransformer<Int>(values, { date, day ->
        date.withDayOfYear(day)
    })

    class ByYearDayFilter(values: List<Int>) : SimpleValueFilter<Int>(values, { it.dayOfYear })

    class ByMonthDayFilter(values: List<Int>) : SimpleValueFilter<Int>(values, { it.dayOfMonth })

    class ByWeekDayFilter(private val values: List<WeekDayOccurrence>) : OccurrenceTransformer(), Filter {
        override fun transform(currentDateTime: List<ZonedDateTime>): List<ZonedDateTime> {
            return currentDateTime.filter { date ->
                val dayOfWeek = date.dayOfWeek
                values.any { it.weekDay.asDayOfWeek() == dayOfWeek }
            }
        }
    }

    class ByWeekDayWeekTransformer(private val weekStart: DayOfWeek, values: List<WeekDayOccurrence>) :
        SimpleValueTransformer<WeekDayOccurrence>(values, { date, weekDay ->
            val startOfWeek = date.with(TemporalAdjusters.previousOrSame(weekStart))
            startOfWeek.with(TemporalAdjusters.nextOrSame(weekDay.weekDay.asDayOfWeek()))
        }), Transformer

    class ByWeekDayMonthlyTransformer(private val values: List<WeekDayOccurrence>) : OccurrenceTransformer(), Transformer {
        override fun transform(currentDateTime: List<ZonedDateTime>): List<ZonedDateTime> {
            return currentDateTime.flatMap { date ->
                val eachInstanceTransformations = values.filter { it.occurrence == null }.flatMap {
                    val weekDay = it.weekDay
                    val firstDayOfMonth = date.with(TemporalAdjusters.firstDayOfMonth())
                    val lastDayOfMonth = date.with(TemporalAdjusters.lastDayOfMonth())
                    val list: MutableList<ZonedDateTime> = mutableListOf()
                    var current = firstDayOfMonth.with(TemporalAdjusters.dayOfWeekInMonth(1, weekDay.asDayOfWeek()))
                    do {
                        list.add(current)
                        current = current.plusWeeks(1)
                    } while (!current.isAfter(lastDayOfMonth))

                    list
                }

                val specificInstanceTransformations = values.filter { it.occurrence != null }.map {
                    date.with(TemporalAdjusters.dayOfWeekInMonth(it.occurrence!!, it.weekDay.asDayOfWeek()))
                }

                eachInstanceTransformations + specificInstanceTransformations
            }
        }
    }

    class ByWeekDayYearlyTransformer(private val values: List<WeekDayOccurrence>) : OccurrenceTransformer(), Transformer {
        override fun transform(currentDateTime: List<ZonedDateTime>): List<ZonedDateTime> {
            return currentDateTime.flatMap { date ->
                val eachInstanceTransformations = values.filter { it.occurrence == null }.flatMap {
                    val weekDay = it.weekDay
                    val firstDay = date.with(TemporalAdjusters.firstDayOfYear())
                    val lastDay = date.with(TemporalAdjusters.lastDayOfYear())
                    val list: MutableList<ZonedDateTime> = mutableListOf()
                    var current = firstDay.with(TemporalAdjusters.nextOrSame(weekDay.asDayOfWeek()))
                    do {
                        list.add(current)
                        current = current.plusWeeks(1)
                    } while (!current.isAfter(lastDay))

                    list
                }

                val specificInstanceTransformations = values.filter { it.occurrence != null }.map {
                    date.with(TemporalAdjusters.firstDayOfYear()).with(TemporalAdjusters.nextOrSame(it.weekDay.asDayOfWeek()))
                        .plusWeeks(it.occurrence!!.toLong() - 1)
                }

                eachInstanceTransformations + specificInstanceTransformations
            }
        }
    }

    class ByMonthFilter(values: List<Int>) : SimpleValueFilter<Int>(values, { date -> date.monthValue })

    class ByMonthTransformer(values: List<Int>) : SimpleValueTransformer<Int>(values, { date, month ->
        date.withMonth(month)
    })

    class ByWeekNoTransformer(values: List<Int>) : SimpleValueTransformer<Int>(values, { date, weekNo ->
        val currentWeek = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val weekDifference = weekNo - currentWeek
        date.plusWeeks(weekDifference.toLong())
    })

    class ByWeekNoFilter(values: List<Int>) : SimpleValueFilter<Int>(values, { date ->
        date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    })
}
