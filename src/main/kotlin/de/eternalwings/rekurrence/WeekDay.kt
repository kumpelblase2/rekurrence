package de.eternalwings.rekurrence

enum class WeekDay(val shortName: String) {
    Monday("MO"),
    Tuesday("TU"),
    Wednesday("WE"),
    Thursday("TH"),
    Friday("FR"),
    Saturday("SA"),
    Sunday("SU");

    companion object {
        fun fromShortName(name: String): WeekDay? {
            val uppercaseName = name.toUpperCase()
            return values().find { it.shortName == uppercaseName }
        }
    }
}

data class WeekDayOccurrence(val weekDay: WeekDay, val occurrence: Int? = null)
