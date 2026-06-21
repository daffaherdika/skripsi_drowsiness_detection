package com.example.drowsinessdetection.data

data class PerformanceRecord(
    val time: Float,
    val fps: Float,
    val latencyMs: Long,
    val tempCelsius: Float
)
