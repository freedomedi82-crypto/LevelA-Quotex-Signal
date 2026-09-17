package com.levela.quotexsignal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelAEngineTest {
    private fun points(count: Int = 120, stepMs: Long = 5000L): List<ReplayPoint> =
        (0 until count).map { ReplayPoint(1_000_000L + it * stepMs, 100.0 + it * 0.01) }

    @Test
    fun researchEngineAcceptsCleanReplay() {
        val report = LevelAResearchEngine.evaluate(points())
        assertEquals("READY", report.verdict)
        assertTrue(report.coverageScore >= 0.80)
    }

    @Test
    fun marketIntegrityPassesCleanReplay() {
        val report = MarketIntegrityEngine.evaluate(points(10))
        assertEquals("PASS", report.verdict)
        assertEquals(10, report.validPoints)
    }

    @Test
    fun marketIntegrityBlocksInvalidPrice() {
        val data = points(10).toMutableList()
        data[4] = ReplayPoint(data[4].timestamp, 0.0)
        val report = MarketIntegrityEngine.evaluate(data)
        assertEquals("BLOCKED", report.verdict)
        assertEquals(1, report.invalidPrices)
    }

    @Test
    fun csvLoaderParsesHeaderAndData() {
        val parsed = ReplayCsvLoader.parse("timestamp,price\n1000,100.0\n6000,100.2\n")
        assertEquals(2, parsed.points.size)
        assertTrue(parsed.errors.isEmpty())
    }

    @Test
    fun visionEngineRejectsLowQualityFrame() {
        val engine = V15VisionEngine()
        val state = engine.push(V15Frame(1.0, 100.0, 0.5, 0.5, 0.1, 1000L))
        assertEquals(0, state.acceptedFrames)
        assertEquals(1, state.rejectedFrames)
    }
}
