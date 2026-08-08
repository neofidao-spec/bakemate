package com.bakemate.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bakemate.data.local.entity.StarterLog
import kotlinx.coroutines.flow.Flow

@Dao
interface StarterLogDao {

    @Query("SELECT * FROM starter_logs ORDER BY feedingTime DESC")
    fun observeAll(): Flow<List<StarterLog>>

    @Query("SELECT * FROM starter_logs ORDER BY feedingTime DESC LIMIT 1")
    suspend fun getLatest(): StarterLog?

    @Query("SELECT * FROM starter_logs ORDER BY feedingTime DESC LIMIT 1")
    fun observeLatest(): Flow<StarterLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: StarterLog): Long

    @Delete
    suspend fun delete(log: StarterLog)

    @Query("SELECT COUNT(*) FROM starter_logs")
    suspend fun count(): Int
}
