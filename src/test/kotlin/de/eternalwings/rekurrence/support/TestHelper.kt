package de.eternalwings.rekurrence.support

import de.eternalwings.rekurrence.RecurringRule
import org.junit.jupiter.api.Assertions.assertIterableEquals
import java.time.ZoneId
import java.time.ZonedDateTime

private const val BASICALLY_FOREVER = 500000

class OccurrenceTestConfigurator {
    var start: ZonedDateTime = ZonedDateTime.of(1997, 9, 2, 9, 0, 0, 0, ZoneId.of("America/New_York"))
    var rule: RecurringRule? = null
    var result: Iterable<ZonedDateTime> = emptyList()
}

fun testOccurrences(setup: OccurrenceTestConfigurator.() -> Unit) {
    val configurator = OccurrenceTestConfigurator()
    configurator.setup()
    val entries = configurator.rule!!.getNextEntries(configurator.start).toList()
    assertIterableEquals(configurator.result, entries)
}

fun testForever(iterations: Int = BASICALLY_FOREVER, setup: OccurrenceTestConfigurator.() -> Unit) {
    val configurator = OccurrenceTestConfigurator()
    configurator.setup()
    val entries = configurator.rule!!.getNextEntries(configurator.start).take(iterations).toList()
    val expected = configurator.result.take(iterations).toList()
    assertIterableEquals(expected, entries)
}
