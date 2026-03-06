package com.manumnoha.healthbridge.samsung

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

private const val TAG = "SamsungHealthReader"

data class HealthConnectSample(
    val recordedAt: Instant,
    val heartRateBpm: Double? = null,
    val spo2Percent: Double? = null,
    val steps: Long? = null,
    val caloriesKcal: Double? = null,
    val activeCaloriesKcal: Double? = null,
    val distanceMeters: Double? = null,
    val floorsClimbed: Double? = null,
)

data class WellnessPoint(
    val recordedAt: Instant,
    val restingHrBpm: Double? = null,
    val hrvRmssdMs: Double? = null,
    val vo2Max: Double? = null,
    val respiratoryRateBrpm: Double? = null,
    val skinTempCelsius: Double? = null,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val bmrKcal: Double? = null,
)

data class HealthConnectSession(
    val externalId: String,
    val startTime: Instant,
    val endTime: Instant,
    val exerciseType: Int,
    val title: String?,
    val notes: String?,
    val durationMinutes: Int,
)

data class HealthConnectSleep(
    val externalId: String,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Int,
    val totalSleepMinutes: Int?,
    val deepSleepMinutes: Int?,
    val lightSleepMinutes: Int?,
    val remSleepMinutes: Int?,
    val awakeMinutes: Int?,
    val notes: String?,
)

val HEALTH_CONNECT_PERMISSIONS = setOf(
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(OxygenSaturationRecord::class),
    HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(SleepSessionRecord::class),
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(DistanceRecord::class),
    HealthPermission.getReadPermission(FloorsClimbedRecord::class),
    HealthPermission.getReadPermission(RestingHeartRateRecord::class),
    HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
    HealthPermission.getReadPermission(Vo2MaxRecord::class),
    HealthPermission.getReadPermission(RespiratoryRateRecord::class),
    HealthPermission.getReadPermission(BodyTemperatureRecord::class),
    HealthPermission.getReadPermission(WeightRecord::class),
    HealthPermission.getReadPermission(BodyFatRecord::class),
    HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
)

class SamsungHealthReader(context: Context) {

    private val client: HealthConnectClient? = runCatching {
        HealthConnectClient.getOrCreate(context)
    }.getOrElse {
        Log.w(TAG, "Health Connect not available: ${it.message}")
        null
    }

    fun isAvailable(): Boolean = client != null

    suspend fun hasPermissions(): Boolean {
        val c = client ?: return false
        return c.permissionController.getGrantedPermissions()
            .containsAll(HEALTH_CONNECT_PERMISSIONS)
    }

    suspend fun grantedPermissions(): Set<String> {
        val c = client ?: return emptySet()
        return c.permissionController.getGrantedPermissions()
            .intersect(HEALTH_CONNECT_PERMISSIONS)
    }

    /** Read HR + SpO2 + steps + calories in 10-min buckets from [since] to now. */
    suspend fun readSamples(since: Instant): List<HealthConnectSample> {
        val c = client ?: return emptyList()
        val filter = TimeRangeFilter.between(since, Instant.now())
        val samples = mutableMapOf<Instant, HealthConnectSample>()

        // Heart rate
        runCatching {
            c.readRecords(ReadRecordsRequest(HeartRateRecord::class, filter)).records
                .flatMap { it.samples }
                .forEach { s ->
                    val bucket = bucket10min(s.time)
                    samples[bucket] = (samples[bucket] ?: HealthConnectSample(bucket))
                        .copy(heartRateBpm = s.beatsPerMinute.toDouble())
                }
        }.onFailure { Log.w(TAG, "HR read failed: ${it.message}") }

        // SpO2
        runCatching {
            c.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, filter)).records
                .forEach { r ->
                    val bucket = bucket10min(r.time)
                    samples[bucket] = (samples[bucket] ?: HealthConnectSample(bucket))
                        .copy(spo2Percent = r.percentage.value)
                }
        }.onFailure { Log.w(TAG, "SpO2 read failed: ${it.message}") }

        // Steps
        runCatching {
            c.readRecords(ReadRecordsRequest(StepsRecord::class, filter)).records
                .forEach { r ->
                    val bucket = bucket10min(r.startTime)
                    samples[bucket] = (samples[bucket] ?: HealthConnectSample(bucket))
                        .copy(steps = r.count)
                }
        }.onFailure { Log.w(TAG, "Steps read failed: ${it.message}") }

        // Calories
        runCatching {
            c.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, filter)).records
                .forEach { r ->
                    val bucket = bucket10min(r.startTime)
                    samples[bucket] = (samples[bucket] ?: HealthConnectSample(bucket))
                        .copy(caloriesKcal = r.energy.inKilocalories)
                }
        }.onFailure { Log.w(TAG, "Calories read failed: ${it.message}") }

        // Active Calories
        runCatching {
            c.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, filter)).records
                .forEach { r ->
                    val bucket = bucket10min(r.startTime)
                    samples[bucket] = (samples[bucket] ?: HealthConnectSample(bucket))
                        .copy(activeCaloriesKcal = r.energy.inKilocalories)
                }
        }.onFailure { Log.w(TAG, "Active calories read failed: ${it.message}") }

        // Distance
        runCatching {
            c.readRecords(ReadRecordsRequest(DistanceRecord::class, filter)).records
                .forEach { r ->
                    val bucket = bucket10min(r.startTime)
                    samples[bucket] = (samples[bucket] ?: HealthConnectSample(bucket))
                        .copy(distanceMeters = r.distance.inMeters)
                }
        }.onFailure { Log.w(TAG, "Distance read failed: ${it.message}") }

        // Floors
        runCatching {
            c.readRecords(ReadRecordsRequest(FloorsClimbedRecord::class, filter)).records
                .forEach { r ->
                    val bucket = bucket10min(r.startTime)
                    samples[bucket] = (samples[bucket] ?: HealthConnectSample(bucket))
                        .copy(floorsClimbed = r.floors)
                }
        }.onFailure { Log.w(TAG, "Floors read failed: ${it.message}") }

        return samples.values.toList().sortedBy { it.recordedAt }
    }

    /** Read wellness snapshots (resting HR, HRV, VO2max, etc.) from [since] to now. */
    suspend fun readWellness(since: Instant): List<WellnessPoint> {
        val c = client ?: return emptyList()
        val filter = TimeRangeFilter.between(since, Instant.now())
        val points = mutableMapOf<Instant, WellnessPoint>()

        fun bucket(t: Instant) = bucket10min(t)

        runCatching {
            c.readRecords(ReadRecordsRequest(RestingHeartRateRecord::class, filter)).records
                .forEach { r ->
                    val b = bucket(r.time)
                    points[b] = (points[b] ?: WellnessPoint(b)).copy(restingHrBpm = r.beatsPerMinute.toDouble())
                }
        }.onFailure { Log.w(TAG, "Resting HR read failed: ${it.message}") }

        runCatching {
            c.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, filter)).records
                .forEach { r ->
                    val b = bucket(r.time)
                    points[b] = (points[b] ?: WellnessPoint(b)).copy(hrvRmssdMs = r.heartRateVariabilityMillis)
                }
        }.onFailure { Log.w(TAG, "HRV read failed: ${it.message}") }

        runCatching {
            c.readRecords(ReadRecordsRequest(Vo2MaxRecord::class, filter)).records
                .forEach { r ->
                    val b = bucket(r.time)
                    points[b] = (points[b] ?: WellnessPoint(b)).copy(vo2Max = r.vo2MillilitersPerMinuteKilogram)
                }
        }.onFailure { Log.w(TAG, "VO2 max read failed: ${it.message}") }

        runCatching {
            c.readRecords(ReadRecordsRequest(RespiratoryRateRecord::class, filter)).records
                .forEach { r ->
                    val b = bucket(r.time)
                    points[b] = (points[b] ?: WellnessPoint(b)).copy(respiratoryRateBrpm = r.rate)
                }
        }.onFailure { Log.w(TAG, "Respiratory rate read failed: ${it.message}") }

        runCatching {
            c.readRecords(ReadRecordsRequest(BodyTemperatureRecord::class, filter)).records
                .forEach { r ->
                    val b = bucket(r.time)
                    points[b] = (points[b] ?: WellnessPoint(b)).copy(skinTempCelsius = r.temperature.inCelsius)
                }
        }.onFailure { Log.w(TAG, "Body temp read failed: ${it.message}") }

        runCatching {
            c.readRecords(ReadRecordsRequest(WeightRecord::class, filter)).records
                .forEach { r ->
                    val b = bucket(r.time)
                    points[b] = (points[b] ?: WellnessPoint(b)).copy(weightKg = r.weight.inKilograms)
                }
        }.onFailure { Log.w(TAG, "Weight read failed: ${it.message}") }

        runCatching {
            c.readRecords(ReadRecordsRequest(BodyFatRecord::class, filter)).records
                .forEach { r ->
                    val b = bucket(r.time)
                    points[b] = (points[b] ?: WellnessPoint(b)).copy(bodyFatPercent = r.percentage.value)
                }
        }.onFailure { Log.w(TAG, "Body fat read failed: ${it.message}") }

        runCatching {
            c.readRecords(ReadRecordsRequest(BasalMetabolicRateRecord::class, filter)).records
                .forEach { r ->
                    val b = bucket(r.time)
                    points[b] = (points[b] ?: WellnessPoint(b)).copy(bmrKcal = r.basalMetabolicRate.inKilocaloriesPerDay)
                }
        }.onFailure { Log.w(TAG, "BMR read failed: ${it.message}") }

        return points.values.toList().sortedBy { it.recordedAt }
    }

    /** Read exercise sessions from [since] to now. */
    suspend fun readSessions(since: Instant): List<HealthConnectSession> {
        val c = client ?: return emptyList()
        val filter = TimeRangeFilter.between(since, Instant.now())
        return runCatching {
            c.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, filter)).records
                .map { r ->
                    val durationMin = ((r.endTime.epochSecond - r.startTime.epochSecond) / 60).toInt()
                    HealthConnectSession(
                        externalId = "hc_${r.metadata.id}",
                        startTime = r.startTime,
                        endTime = r.endTime,
                        exerciseType = r.exerciseType,
                        title = r.title,
                        notes = r.notes,
                        durationMinutes = durationMin,
                    )
                }
        }.getOrElse {
            Log.w(TAG, "Exercise read failed: ${it.message}")
            emptyList()
        }
    }

    /** Read sleep sessions from [since] to now. */
    suspend fun readSleep(since: Instant): List<HealthConnectSleep> {
        val c = client ?: return emptyList()
        val filter = TimeRangeFilter.between(since, Instant.now())
        return runCatching {
            c.readRecords(ReadRecordsRequest(SleepSessionRecord::class, filter)).records
                .map { r ->
                    val durationMin = ((r.endTime.epochSecond - r.startTime.epochSecond) / 60).toInt()
                    var totalSleep = 0
                    var deep = 0
                    var light = 0
                    var rem = 0
                    var awake = 0
                    r.stages.forEach { stage ->
                        val mins = ((stage.endTime.epochSecond - stage.startTime.epochSecond) / 60).toInt()
                        when (stage.stage) {
                            SleepSessionRecord.STAGE_TYPE_DEEP  -> deep += mins
                            SleepSessionRecord.STAGE_TYPE_LIGHT -> light += mins
                            SleepSessionRecord.STAGE_TYPE_REM   -> rem += mins
                            SleepSessionRecord.STAGE_TYPE_AWAKE -> awake += mins
                            SleepSessionRecord.STAGE_TYPE_SLEEPING -> totalSleep += mins
                        }
                    }
                    // If no explicit "sleeping" stage, sum deep+light+rem as total sleep
                    if (totalSleep == 0) totalSleep = deep + light + rem
                    HealthConnectSleep(
                        externalId = "hc_sleep_${r.metadata.id}",
                        startTime = r.startTime,
                        endTime = r.endTime,
                        durationMinutes = durationMin,
                        totalSleepMinutes = totalSleep.takeIf { it > 0 },
                        deepSleepMinutes = deep.takeIf { it > 0 },
                        lightSleepMinutes = light.takeIf { it > 0 },
                        remSleepMinutes = rem.takeIf { it > 0 },
                        awakeMinutes = awake.takeIf { it > 0 },
                        notes = r.notes,
                    )
                }
        }.getOrElse {
            Log.w(TAG, "Sleep read failed: ${it.message}")
            emptyList()
        }
    }

    private fun bucket10min(t: Instant): Instant {
        val epochSec = t.epochSecond
        return Instant.ofEpochSecond(epochSec - (epochSec % 600))
    }
}
