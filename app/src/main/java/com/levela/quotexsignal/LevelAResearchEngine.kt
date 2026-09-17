package com.levela.quotexsignal

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.round
import kotlin.math.sqrt

data class ReplayPoint(val timestamp: Long, val price: Double)
data class LevelAIntegrityReport(val points:Int=0,val validPoints:Int=0,val duplicateTimestamps:Int=0,val invalidPrices:Int=0,val nonMonotonicTimestamps:Int=0,val medianIntervalMs:Long=0,val intervalCv:Double=0.0,val medianAbsReturn:Double=0.0,val p95AbsReturn:Double=0.0,val maxAbsReturn:Double=0.0,val jumpCount:Int=0,val suggestedExpirySteps:Int=3,val suggestedExpirySeconds:Int=60,val coverageScore:Double=0.0,val verdict:String="BLOCKED",val note:String="Belum diuji",val issues:List<String> = emptyList())

object LevelAResearchEngine {
    fun evaluate(points: List<ReplayPoint>, requestedExpirySeconds: Int = 60): LevelAIntegrityReport {
        if (points.isEmpty()) return LevelAIntegrityReport(note="Belum ada data replay")
        var dup=0; var invalid=0; var nonMono=0
        for(i in 1 until points.size){ if(points[i].timestamp==points[i-1].timestamp) dup++; if(points[i].timestamp<points[i-1].timestamp) nonMono++ }
        val sorted=points.sortedBy { it.timestamp }; invalid=sorted.count { !it.price.isFinite() || it.price<=0.0 }
        val clean=sorted.filter { it.price.isFinite() && it.price>0.0 }
        if(clean.size<20) return LevelAIntegrityReport(points.size,clean.size,dup,invalid,nonMono,note="Data valid < 20 titik; belum layak dikalibrasi",issues=listOf("DATA_TOO_SMALL"))
        val intervals=clean.zipWithNext().map { it.second.timestamp-it.first.timestamp }.filter { it>0 }; val medInt=medianLong(intervals); val mean=intervals.average(); val sd=sqrt(intervals.map { (it-mean)*(it-mean) }.average()); val cv=if(mean>0) sd/mean else Double.POSITIVE_INFINITY
        val returns=clean.zipWithNext().map { abs((it.second.price-it.first.price)/it.first.price) }.filter { it.isFinite() }; val medRet=median(returns); val p95=percentile(returns,.95); val maxRet=returns.maxOrNull()?:0.0; val threshold=maxOf(medRet*8,p95*1.5,.0005); val jumps=returns.count { it>threshold }
        val steps=if(medInt>0) round(requestedExpirySeconds.coerceAtLeast(1)*1000.0/medInt).toInt().coerceIn(1,120) else 3; val seconds=((steps.toLong()*medInt)/1000L).toInt().coerceAtLeast(1)
        val continuity=when { cv<=.25->1.0; cv<=.50->.8; cv<=1.0->.55; else->.25 }; val jumpPenalty=(jumps.toDouble()/returns.size.coerceAtLeast(1)).coerceAtMost(1.0); val uniqueness=1.0-((dup+nonMono).toDouble()/points.size).coerceIn(0.0,1.0); val validity=clean.size.toDouble()/points.size; val coverage=((continuity*.45)+(uniqueness*.25)+(validity*.20)+(1-jumpPenalty)*.10).coerceIn(0.0,1.0)
        val issues=mutableListOf<String>(); if(dup>0)issues+="DUPLICATE_TIMESTAMP=$dup"; if(nonMono>0)issues+="NON_MONOTONIC_TIMESTAMP=$nonMono"; if(invalid>0)issues+="INVALID_PRICE=$invalid"; if(cv>.75)issues+="IRREGULAR_SAMPLING_CV=${"%.2f".format(cv)}"; if(jumpPenalty>.03)issues+="EXCESSIVE_JUMPS=${"%.1f".format(jumpPenalty*100)}%"
        val verdict=when { invalid>0||nonMono>0||coverage<.45->"BLOCKED"; clean.size>=100&&coverage>=.80&&cv<=.50&&jumpPenalty<=.03->"READY"; clean.size>=50&&coverage>=.65->"USABLE"; else->"FRAGILE" }
        return LevelAIntegrityReport(points.size,clean.size,dup,invalid,nonMono,medInt,cv,medRet,p95,maxRet,jumps,steps,seconds,coverage,verdict,"Sampling median ${medInt}ms • target ${requestedExpirySeconds}s → ${seconds}s (${steps} steps) • coverage ${"%.0f".format(coverage*100)}%",issues)
    }
    private fun median(v:List<Double>)=if(v.isEmpty())0.0 else v.sorted()[v.size/2]
    private fun medianLong(v:List<Long>)=if(v.isEmpty())0L else v.sorted()[v.size/2]
    private fun percentile(v:List<Double>,p:Double):Double { if(v.isEmpty())return 0.0; val s=v.sorted(); val idx=((s.size-1)*p).coerceIn(0.0,(s.size-1).toDouble()); val lo=idx.toInt(); val hi=ceil(idx).toInt(); return if(lo==hi)s[lo] else s[lo]+(s[hi]-s[lo])*(idx-lo) }
}
