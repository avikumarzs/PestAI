package com.avikshit.PestAI

object SprayEngine {
    enum class Status { PERFECT, WARNING, DO_NOT_SPRAY }
    data class SprayDecision(val status: Status, val message: String)
    fun evaluateConditions(windSpeedKmh: Double, tempCelsius: Double, isRaining: Boolean): SprayDecision {
        if (isRaining) return SprayDecision(Status.DO_NOT_SPRAY, "Rain detected. Chemicals will wash away.")
        if (windSpeedKmh > 15.0) return SprayDecision(Status.DO_NOT_SPRAY, "High wind (${windSpeedKmh}km/h). Severe risk of chemical drift.")
        if (tempCelsius > 30.0) return SprayDecision(Status.WARNING, "High heat (${tempCelsius}°C). Chemicals may evaporate quickly.")
        if (tempCelsius < 5.0) return SprayDecision(Status.WARNING, "Too cold (${tempCelsius}°C). Pests are inactive, spray is less effective.")
        return SprayDecision(Status.PERFECT, "Weather is optimal for spraying.")
    }
}