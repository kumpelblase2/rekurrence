package de.eternalwings.rekurrence

import de.eternalwings.rekurrence.OccurrenceTransformer.ByHourFilter
import de.eternalwings.rekurrence.OccurrenceTransformer.ByHourTransformer
import de.eternalwings.rekurrence.OccurrenceTransformer.ByMinuteFilter
import de.eternalwings.rekurrence.OccurrenceTransformer.ByMinuteTransformer
import de.eternalwings.rekurrence.OccurrenceTransformer.ByMonthDayFilter
import de.eternalwings.rekurrence.OccurrenceTransformer.ByMonthDayTransformer
import de.eternalwings.rekurrence.OccurrenceTransformer.ByMonthFilter
import de.eternalwings.rekurrence.OccurrenceTransformer.ByMonthTransformer
import de.eternalwings.rekurrence.OccurrenceTransformer.BySecondFilter
import de.eternalwings.rekurrence.OccurrenceTransformer.BySecondTransformer
import de.eternalwings.rekurrence.OccurrenceTransformer.ByWeekDayFilter
import de.eternalwings.rekurrence.OccurrenceTransformer.ByWeekDayMonthlyTransformer
import de.eternalwings.rekurrence.OccurrenceTransformer.ByWeekDayWeekTransformer
import de.eternalwings.rekurrence.OccurrenceTransformer.ByWeekDayYearlyTransformer
import de.eternalwings.rekurrence.OccurrenceTransformer.ByWeekNoFilter
import de.eternalwings.rekurrence.OccurrenceTransformer.ByWeekNoTransformer
import de.eternalwings.rekurrence.OccurrenceTransformer.ByYearDayFilter
import de.eternalwings.rekurrence.OccurrenceTransformer.ByYearDayTransformer
import de.eternalwings.rekurrence.Frequency.HOURLY
import de.eternalwings.rekurrence.Frequency.MINUTELY
import de.eternalwings.rekurrence.Frequency.MONTHLY
import de.eternalwings.rekurrence.Frequency.SECONDLY
import de.eternalwings.rekurrence.Frequency.WEEKLY
import de.eternalwings.rekurrence.Frequency.YEARLY
import java.time.ZonedDateTime

class GeneratorList(rule: RecurringRule, private val startingInstance: ZonedDateTime) {
    private val transformers: List<OccurrenceTransformer>
    private val frequencyGenerator = FrequencyGenerator(rule.frequency, rule.interval.toLong(), startingInstance)

    init {
        val transformerList = mutableListOf<OccurrenceTransformer>()

        if (rule.bySecond.isNotEmpty()) {
            if(rule.frequency == SECONDLY) {
                transformerList.add(BySecondFilter(rule.bySecond))
            } else {
                transformerList.add(BySecondTransformer(rule.bySecond))
            }
        }

        if (rule.byMinute.isNotEmpty()) {
            if(rule.frequency in setOf(SECONDLY, MINUTELY)) {
                transformerList.add(ByMinuteFilter(rule.byMinute))
            } else {
                transformerList.add(ByMinuteTransformer(rule.byMinute))
            }
        }

        if (rule.byHour.isNotEmpty()) {
            if(rule.frequency in setOf(SECONDLY, MINUTELY, HOURLY)) {
                transformerList.add(ByHourFilter(rule.byHour))
            } else {
                transformerList.add(ByHourTransformer(rule.byHour))
            }
        }

        if (rule.byWeekDay.isNotEmpty()) {
            if (rule.frequency != WEEKLY && rule.frequency != MONTHLY && rule.frequency != YEARLY) {
                transformerList.add(ByWeekDayFilter(rule.byWeekDay))
            } else if (rule.frequency == MONTHLY || rule.byMonth.isNotEmpty()) {
                transformerList.add(ByWeekDayMonthlyTransformer(rule.byWeekDay))
            } else if (rule.frequency == YEARLY) {
                transformerList.add(ByWeekDayYearlyTransformer(rule.byWeekDay))
            } else { // i.e. WEEKLY frequency
                transformerList.add(ByWeekDayWeekTransformer(rule.weekStart.asDayOfWeek(), rule.byWeekDay))
            }
        }

        if (rule.byMonthDay.isNotEmpty()) {
            if (rule.frequency !in setOf(YEARLY, MONTHLY) || rule.byWeekDay.isNotEmpty()) { // TODO is this condition correct?
                transformerList.add(ByMonthDayFilter(rule.byMonthDay))
            } else {
                transformerList.add(ByMonthDayTransformer(rule.byMonthDay))
            }
        }

        if (rule.byYearDay.isNotEmpty()) {
            if (rule.frequency != WEEKLY && rule.frequency != MONTHLY && rule.frequency != YEARLY) {
                transformerList.add(ByYearDayFilter(rule.byYearDay))
            } else {
                transformerList.add(ByYearDayTransformer(rule.byYearDay))
            }
        }

        if (rule.byWeekNr.isNotEmpty()) {
            // TODO: in the spec they have this:
            //  The numeric value in a
            //      BYDAY rule part with the FREQ rule part set to YEARLY corresponds
            //      to an offset within the month when the BYMONTH rule part is
            //      present, and corresponds to an offset within the year when the
            //      BYWEEKNO or BYMONTH rule parts are present.
            //  Which doesn't make that much sense
            if (rule.frequency != YEARLY || rule.byWeekDay.isNotEmpty()) {
                transformerList.add(ByWeekNoFilter(rule.byWeekNr))
            } else {
                transformerList.add(ByWeekNoTransformer(rule.byWeekNr))
            }
        }

        if (rule.byMonth.isNotEmpty()) {
            if (rule.frequency !in setOf(YEARLY, MONTHLY)) {
                transformerList.add(ByMonthFilter(rule.byMonth))
            } else { // i.e. frequency is YEARLY
                transformerList.add(ByMonthTransformer(rule.byMonth))
            }
        }

        // TODO bySetPosition

        transformers = transformerList
    }

    private fun runTransformers(nextFrequencyValues: List<ZonedDateTime>): List<ZonedDateTime> {
        return transformers.fold(nextFrequencyValues) { current, transformer ->
            transformer.transform(current)
        }.sorted()
    }

    fun generateNext(): List<ZonedDateTime> {
        var generatedDateTimes: List<ZonedDateTime>
        do {
            val frequencyValues = frequencyGenerator.nextValues()
            val transformedValues = runTransformers(frequencyValues)
            generatedDateTimes = transformedValues.filter { it.isEqual(startingInstance) || it.isAfter(startingInstance) }
        } while (generatedDateTimes.isEmpty())
        return generatedDateTimes
    }
}
