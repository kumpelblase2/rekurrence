package de.eternalwings.rekurrence

import java.time.DayOfWeek

enum class WeekDay(val shortName: String) {
    Monday("MO"),
    Tuesday("TU"),
    Wednesday("WE"),
    Thursday("TH"),
    Friday("FR"),
    Saturday("SA"),
    Sunday("SU");

    fun asDayOfWeek(): DayOfWeek {
        return DayOfWeek.valueOf(name.toUpperCase())
    }

    companion object {
        fun fromShortName(name: String): WeekDay? {
            val uppercaseName = name.toUpperCase()
            return values().find { it.shortName == uppercaseName }
        }
    }
}

data class WeekDayOccurrence(val weekDay: WeekDay, val occurrence: Int? = null)
