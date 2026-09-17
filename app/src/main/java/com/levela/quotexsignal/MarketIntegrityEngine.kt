package com.levela.quotexsignal

import kotlin.math.abs

/**
 * Lightweight, deterministic market-data gate used before replay/calibration.
 * It does not produce trade orders; it only classifies feed integrity.
 */
data class MarketIntegrityReport(
    val points: Int,
    val validPoints: Int,
    val duplicateTimestamps: Int,
    val nonMonotonicTimestamps: Int,
    val invalidPrices: Int,
    val staleIntervals: Int,
    val maxGapMs: Long,
    val verdict: String,
    val issues: List<String>
)

object MarketIntegrityEngine {
    fun evaluate(points: List<ReplayPoint>, maxGapMs: Long = 30_000L): MarketIntegrityReport {
        if (points.isEmpty()) {
            return MarketIntegrityReport(0, 0, 0, 0, 0, 0, 0L, "BLOCKED", listOf("NO_DATA"))
        }
        var duplicates = 0
        var nonMonotonic = 0
        var stale = 0
        var maxGap = 0L
        for (i in 1 until points.size) {
            val gap = points[i].timestamp - points[i - 1].timestamp
            if (gap == 0L) duplicates++
            if (gap < 0L) nonMonotonic++
            if (gap > maxGap) maxGap = gap
            if (gap > maxGapMs) stale++
        }
        val invalid = points.count { !it.price.isFinite() || it.price <= 0.0 }
        val valid = points.count { it.price.isFinite() && it.price > 0.0 }
        val issues = mutableListOf<String>()
        if (duplicates > 0) issues += "DUPLICATE_TIMESTAMP=$duplicates"
        if (nonMonotonic > 0) issues += "NON_MONOTONIC_TIMESTAMP=$nonMonotonic"
        if (invalid > 0) issues += "INVALID_PRICE=$invalid"
        if (stale > 0) issues += "STALE_GAP=$stale"
        val verdict = when {
            invalid > 0 || nonMonotonic > 0 -> "BLOCKED"
            valid < 2 -> "BLOCKED"
            duplicates > 0 || stale > 0 -> "DEGRADED"
            else -> "PASS"
        }
        return MarketIntegrityReport(points.size, valid, duplicates, nonMonotonic, invalid, stale, maxGap, verdict, issues)
    }

    fun priceJumpRatio(previous: Double, current: Double): Double {
        if (!previous.isFinite() || previous <= 0.0 || !current.isFinite()) return Double.POSITIVE_INFINITY
        return abs(current - previous) / previous
    }
}
