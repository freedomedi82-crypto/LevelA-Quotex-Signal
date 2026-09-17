package com.levela.quotexsignal

import android.content.Context
import java.io.File

class LevelADataCapture(private val context: Context) {
    private val file: File = File(context.filesDir, "level_a_capture.csv")
    private var active = false

    fun start(reset: Boolean = false) {
        if (reset || !file.exists()) file.writeText("timestamp,price\n")
        active = true
    }

    fun stop() { active = false }
    fun isActive(): Boolean = active

    fun capture(price: Double, timestamp: Long = System.currentTimeMillis()): Boolean {
        if (!active || !price.isFinite() || price <= 0.0) return false
        file.appendText("$timestamp,$price\n")
        return true
    }

    fun csvText(): String = if (file.exists()) file.readText() else "timestamp,price\n"
    fun pointCount(): Int = ReplayCsvLoader.parse(csvText()).points.size
    fun filePath(): String = file.absolutePath
}
