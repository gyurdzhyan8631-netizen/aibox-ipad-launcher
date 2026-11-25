package com.aibox.launcher.weather

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/** Расчёт восхода и заката по координатам (без интернета). */
object SunCalc {

    /**
     * Восход и закат в минутах от полуночи по местному времени.
     * null — в этот день солнце не восходит или не заходит (полярные широты).
     */
    fun sunriseSunset(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): Pair<Int, Int>? {
        val rise = compute(latitude, longitude, date, zone, rising = true) ?: return null
        val set = compute(latitude, longitude, date, zone, rising = false) ?: return null
        return rise to set
    }

    private fun compute(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        zone: ZoneId,
        rising: Boolean
    ): Int? {
        val dayOfYear = date.dayOfYear
        val lngHour = longitude / 15.0
        val t = dayOfYear + ((if (rising) 6.0 else 18.0) - lngHour) / 24.0
        val meanAnomaly = 0.9856 * t - 3.289

        var trueLongitude = meanAnomaly +
            1.916 * sin(Math.toRadians(meanAnomaly)) +
            0.020 * sin(Math.toRadians(2 * meanAnomaly)) +
            282.634
        trueLongitude = normalize(trueLongitude, 360.0)

        var rightAscension = Math.toDegrees(atan(0.91764 * tan(Math.toRadians(trueLongitude))))
        rightAscension = normalize(rightAscension, 360.0)
        val longitudeQuadrant = floor(trueLongitude / 90.0) * 90.0
        val ascensionQuadrant = floor(rightAscension / 90.0) * 90.0
        rightAscension = (rightAscension + (longitudeQuadrant - ascensionQuadrant)) / 15.0

        val sinDeclination = 0.39782 * sin(Math.toRadians(trueLongitude))
        val cosDeclination = cos(asin(sinDeclination))

        val zenith = Math.toRadians(90.833)
        val latRad = Math.toRadians(latitude)
        val cosHourAngle = (cos(zenith) - sinDeclination * sin(latRad)) / (cosDeclination * cos(latRad))
        if (cosHourAngle > 1.0 || cosHourAngle < -1.0) return null

        val hourAngleDegrees = if (rising) {
            360.0 - Math.toDegrees(acos(cosHourAngle))
        } else {
            Math.toDegrees(acos(cosHourAngle))
        }
        val hourAngle = hourAngleDegrees / 15.0

        val localMeanTime = hourAngle + rightAscension - 0.06571 * t - 6.622
        val universalTime = normalize(localMeanTime - lngHour, 24.0)
        val offsetHours = zone.rules
            .getOffset(date.atStartOfDay(zone).toInstant())
            .totalSeconds / 3600.0
        val local = normalize(universalTime + offsetHours, 24.0)
        return (local * 60.0).roundToInt() % 1440
    }

    private fun normalize(value: Double, range: Double): Double {
        var v = value % range
        if (v < 0) v += range
        return v
    }
}
