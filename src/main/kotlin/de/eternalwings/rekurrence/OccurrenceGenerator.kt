package de.eternalwings.rekurrence

import java.time.ZonedDateTime
import java.util.LinkedList
import java.util.Queue

object OccurrenceGenerator {
    fun generateFrom(rule: RecurringRule, startingInstance: ZonedDateTime): Sequence<ZonedDateTime> {
        if (rule.interval == 0) {
            return emptySequence()
        }

        val tree = GeneratorList(rule, startingInstance)
        val remaining: Queue<ZonedDateTime> = LinkedList()
        var count = 0

        return generateSequence {
            if (rule.count != null && ++count > rule.count) {
                return@generateSequence null
            }
            if (remaining.isEmpty()) {
                val nextElements = tree.generateNext()
                if (nextElements.isEmpty()) {
                    return@generateSequence null
                }
                // TODO: this should be sorted correctly since we can
                //  technically also run backwards
                remaining.addAll(nextElements)
            }
            val result = remaining.poll()
            if (rule.endDate != null && result.isAfter(rule.endDate)) {
                return@generateSequence null
            }
            result
        }
    }
}
