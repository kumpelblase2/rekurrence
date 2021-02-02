package de.eternalwings.rekurrence

import de.eternalwings.rekurrence.support.RFCTestCase
import de.eternalwings.rekurrence.support.RecurringRuleRFCTestInvocationContextProvider
import de.eternalwings.rekurrence.support.testForever
import de.eternalwings.rekurrence.support.testOccurrences
import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.recur.RecurrenceRule
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import java.time.ZonedDateTime
import java.util.TimeZone

class RecurringRuleRFCTestSuite {
    private fun DateTime.toJavaDateTime(): ZonedDateTime {
        val zone = this.timeZone.toZoneId()
        return ZonedDateTime.of(this.year, this.month + 1, this.dayOfMonth, this.hours, this.minutes, this.seconds, 0, zone)
    }

    private fun ZonedDateTime.toRecurDateTime(): DateTime {
        return DateTime(
            TimeZone.getTimeZone(this.zone.id),
            this.year,
            this.monthValue - 1,
            this.dayOfMonth,
            this.hour,
            this.minute,
            this.second
        )
    }

    @TestTemplate
    @ExtendWith(RecurringRuleRFCTestInvocationContextProvider::class)
    fun testRFCTestTemplate(spec: RFCTestCase) {
        if (spec.infinite) {
            return spec.rules.forEach { rule ->
                val otherRule = RecurrenceRule(rule.asString())
                val otherIterator = otherRule.iterator(spec.start.toRecurDateTime())
                testForever(10000) {
                    start = spec.start
                    this.rule = rule
                    result = generateSequence {
                        otherIterator.nextDateTime().toJavaDateTime()
                    }.asIterable()
                }
            }
        } else {
            spec.rules.forEach { rule ->
                testOccurrences {
                    start = spec.start
                    this.rule = rule
                    result = spec.results
                }
            }
        }
    }
}

