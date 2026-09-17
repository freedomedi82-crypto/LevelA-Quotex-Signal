package com.levela.quotexsignal

object ReplayCsvLoader {
    data class Result(val points: List<ReplayPoint>, val errors: List<String>)

    fun parse(text: String): Result {
        val points = mutableListOf<ReplayPoint>()
        val errors = mutableListOf<String>()
        text.lineSequence().forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
            val parts = line.split(',', ';', '\t').map { it.trim() }
            if (parts.size < 2) {
                if (index > 0) errors += "LINE_${index + 1}_MISSING_COLUMNS"
                return@forEachIndexed
            }
            if (index == 0 && parts[0].equals("timestamp", true)) return@forEachIndexed
            val timestamp = parts[0].toLongOrNull()
            val price = parts[1].toDoubleOrNull()
            if (timestamp == null || price == null) {
                errors += "LINE_${index + 1}_INVALID_NUMBER"
            } else {
                points += ReplayPoint(timestamp, price)
            }
        }
        return Result(points, errors)
    }
}
