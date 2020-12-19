package de.eternalwings.rekurrence

import de.eternalwings.rekurrence.Frequency.*
import java.time.ZonedDateTime
import kotlin.math.ceil

class IntervalContainer(rule: RecurringRule) {
    private var iterationIndex = 0
    private val offsets: List<OffsetApplier>

    init {
        val staticOffset: (ZonedDateTime, Long) -> ZonedDateTime = when (rule.frequency) {
            SECONDLY -> ({ date, amount -> date.plusSeconds(rule.interval * amount) })
            MINUTELY -> ({ date, amount -> date.plusMinutes(rule.interval * amount) })
            HOURLY -> ({ date, amount -> date.plusHours(rule.interval * amount) })
            DAILY -> ({ date, amount -> date.plusDays(rule.interval * amount) })
            WEEKLY -> ({ date, amount -> date.plusWeeks(rule.interval * amount) })
            MONTHLY -> ({ date, amount -> date.plusMonths(rule.interval * amount) })
            YEARLY -> ({ date, amount -> date.plusYears(rule.interval * amount) })
        }
        var currentList = listOf(OffsetApplier(staticOffset))

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
