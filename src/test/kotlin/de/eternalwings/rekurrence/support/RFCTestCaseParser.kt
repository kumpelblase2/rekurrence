package de.eternalwings.rekurrence.support

import de.eternalwings.rekurrence.RecurringRule
import de.eternalwings.rekurrence.support.ParserState.EXPECTING_PROPERTIES
import de.eternalwings.rekurrence.support.ParserState.EXPECTING_RULE
import de.eternalwings.rekurrence.support.ParserState.RESULTS
import de.eternalwings.rekurrence.support.RFCTestCase.Builder
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

data class RFCTestCase(
    val name: String,
    val start: ZonedDateTime,
    val rules: List<RecurringRule>,
    val results: List<ZonedDateTime>,
    val infinite: Boolean = false
) {
    class Builder {
        var name: String? = null
        var start: ZonedDateTime? = null
        var rules: List<RecurringRule> = emptyList()
        var results: List<ZonedDateTime> = emptyList()
        var infinite: Boolean = false

        fun build(): RFCTestCase {
            return RFCTestCase(name!!, start!!, rules, results, infinite)
        }
    }
}

internal enum class ParserState {
    EXPECTING_RULE,
    EXPECTING_PROPERTIES,
    RESULTS
}

object RFCTestCaseParser {
    private val START_REGEX = "DTSTART;TZID=([^/]+/[^/]+):([\\dT]+)".toRegex()
    private val START_DATETIME_FORMATTER =
        DateTimeFormatterBuilder().appendValue(ChronoField.YEAR, 4).appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral('T').appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendValue(ChronoField.SECOND_OF_MINUTE, 2).toFormatter()
    private val RESULT_REGEX = "(\\((\\d{4}) ((\\d{1,2}):(\\d{2}) (AM|PM) )?\\w{3}\\))? *(.*)".toRegex()

    private fun parseStartTime(string: String): ZonedDateTime {
        val startMatch = START_REGEX.matchEntire(string) ?: throw IllegalArgumentException()
        val timezone = ZoneId.of(startMatch.groupValues[1])
        return ZonedDateTime.parse(startMatch.groupValues[2], START_DATETIME_FORMATTER.withZone(timezone))
    }

    private fun parseRules(definedRules: List<String>): List<RecurringRule> {
        return definedRules.filter { it.trim().startsWith("RRULE") }.map { rule ->
            val ruleText = rule.substringAfter("RRULE:")
            RecurringRule.fromString(ruleText)
        }
    }

    private fun parseMonth(month: String): Int {
        return when (month) {
            "January" -> 1
            "February" -> 2
            "March" -> 3
            "April" -> 4
            "May" -> 5
            "June" -> 6
            "July" -> 7
            "August" -> 8
            "September" -> 9
            "October" -> 10
            "November" -> 11
            "December" -> 12
            else -> throw IllegalArgumentException()
        }
    }

    fun parse(text: String): List<RFCTestCase> {
        var remainingText = text
        var state = EXPECTING_RULE
        val rules = mutableListOf<RFCTestCase>()
        var currentBuilder = Builder()
        while (remainingText.isNotBlank()) {
            when (state) {
                EXPECTING_RULE -> {
                    currentBuilder.name = remainingText.substringBefore(':').trim()
                    remainingText = remainingText.substringAfter(':')
                    state = EXPECTING_PROPERTIES
                }
                EXPECTING_PROPERTIES -> {
                    val props = remainingText.substringBefore("==>").trim()
                    val lines = props.split('\n').filter { it.isNotBlank() || it.trim() == "or" }
                    val start = lines[0]
                    val definedRules = lines.subList(1, lines.size)
                    currentBuilder.start = parseStartTime(start)
                    currentBuilder.rules = parseRules(definedRules)
                    remainingText = remainingText.substringAfter("==>")
                    state = RESULTS
                }
                RESULTS -> {
                    val resultsString = remainingText.substringBefore("\n\n").trim()
                    remainingText = if(!remainingText.contains("\n\n")) {
                        ""
                    } else {
                        remainingText.substringAfter("\n\n")
                    }

                    val resultLines = resultsString.split('\n').map { it.trim() }

                    var currentYear = 1997
                    var currentHour = 9
                    var currentMinute = 0
                    val results = mutableListOf<ZonedDateTime>()
                    for (line in resultLines) {
                        if(line.trim() == "...") {
                            currentBuilder.infinite = true
                            break
                        }

                        val regexMatch = RESULT_REGEX.matchEntire(line) ?: throw IllegalStateException()
                        val groupMatches = regexMatch.groupValues
                        if (groupMatches[1].isNotBlank()) {
                            currentYear = groupMatches[2].toInt()
                            if(groupMatches[3].isNotBlank()) {
                                currentHour = if (groupMatches[6] == "AM") groupMatches[4].toInt() else groupMatches[4].toInt()+ 12
                                currentMinute = groupMatches[5].toInt()
                            } else {
                                currentHour = currentBuilder.start!!.hour
                                currentMinute = currentBuilder.start!!.minute
                            }
                        }
                        val monthsDays = groupMatches[7].split(";").filter { it.isNotBlank() }
                        for (monthWithDays in monthsDays) {

                            val (monthString, daySpecs) = monthWithDays.trim().split(' ', limit = 2)
                            val month = parseMonth(monthString)
                            val daySpecsSplit = daySpecs.split(", ?".toRegex())
                            val days = daySpecsSplit.flatMapIndexed { index, spec ->
                                when {
                                    spec.endsWith("...") -> {
                                        currentBuilder.infinite = true // This is a bit ... poopy
                                        emptyList()
                                    }
                                    spec.contains("...") -> {
                                        val (startText, endText) = spec.split("...")
                                        val start = startText.toInt()
                                        val end = endText.toInt()
                                        val previous = daySpecsSplit[index - 1].toInt()
                                        val difference = start - previous
                                        generateSequence(start) {
                                            val next = it + difference
                                            if (next > end) {
                                                null
                                            } else {
                                                next
                                            }
                                        }.toList()
                                    }
                                    spec.contains("-") -> {
                                        val (start, end) = spec.split('-')
                                        IntRange(start.toInt(), end.toInt()).toList()
                                    }
                                    else -> listOf(spec.toInt())
                                }
                            }

                            results.addAll(days.map { day ->
                                ZonedDateTime.of(
                                    currentYear,
                                    month,
                                    day,
                                    currentHour,
                                    currentMinute,
                                    0,
                                    0,
                                    currentBuilder.start!!.zone
                                )
                            })
                        }
                    }

                    currentBuilder.results = results
                    rules.add(currentBuilder.build())
                    currentBuilder = Builder()
                    state = EXPECTING_RULE
                }
            }
        }

        return rules
    }
}
