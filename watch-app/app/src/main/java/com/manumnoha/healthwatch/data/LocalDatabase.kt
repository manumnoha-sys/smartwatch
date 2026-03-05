package com.manumnoha.healthwatch.data

import android.content.Context
import androidx.room.*

@Dao
interface SensorReadingDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(reading: SensorReading)

    @Query("SELECT * FROM sensor_readings WHERE synced = 0 ORDER BY recordedAt ASC LIMIT 500")
    suspend fun getPending(): List<SensorReading>

    @Query("UPDATE sensor_readings SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("DELETE FROM sensor_readings WHERE synced = 1 AND recordedAt < :beforeMs")
    suspend fun deleteSynced(beforeMs: Long)
}

@Database(entities = [SensorReading::class], version = 1, exportSchema = false)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun readingDao(): SensorReadingDao

    companion object {
        @Volatile private var INSTANCE: LocalDatabase? = null

        fun get(context: Context): LocalDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalDatabase::class.java,
                    "health_watch.db"
                ).build().also { INSTANCE = it }
            }
    }
}
