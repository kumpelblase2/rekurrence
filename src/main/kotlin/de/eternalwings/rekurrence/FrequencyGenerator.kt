package de.eternalwings.rekurrence

import de.eternalwings.rekurrence.Frequency.DAILY
import de.eternalwings.rekurrence.Frequency.HOURLY
import de.eternalwings.rekurrence.Frequency.MINUTELY
import de.eternalwings.rekurrence.Frequency.MONTHLY
import de.eternalwings.rekurrence.Frequency.SECONDLY
import de.eternalwings.rekurrence.Frequency.WEEKLY
import de.eternalwings.rekurrence.Frequency.YEARLY
import java.time.ZonedDateTime

class FrequencyGenerator(
    frequency: Frequency,
    private val interval: Long,
    startingInstance: ZonedDateTime,
    private val amount: Int = 30
) {
    private var current = startingInstance
    private val offsetGenerator: (ZonedDateTime) -> ZonedDateTime = when (frequency) {
        SECONDLY -> ({ date -> date.plusSeconds(interval) })
        MINUTELY -> ({ date -> date.plusMinutes(interval) })
        HOURLY -> ({ date -> date.plusHours(interval) })
        DAILY -> ({ date -> date.plusDays(interval) })
        WEEKLY -> ({ date -> date.plusWeeks(interval) })
        MONTHLY -> ({ date -> date.plusMonths(interval) })
        YEARLY -> ({ date -> date.plusYears(interval) })
    }

    fun nextValues(): List<ZonedDateTime> {
        val entries = ArrayList<ZonedDateTime>(amount)
        for (i in 0..amount) {
            entries.add(current)
            current = offsetGenerator(current)
        }

        return entries
    }
}
