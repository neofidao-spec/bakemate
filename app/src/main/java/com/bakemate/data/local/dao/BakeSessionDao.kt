package com.bakemate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bakemate.data.local.entity.BakeSession
import kotlinx.coroutines.flow.Flow

@Dao
interface BakeSessionDao {

    @Query("SELECT * FROM bake_sessions WHERE status != 'DONE' ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<BakeSession?>

    @Query("SELECT * FROM bake_sessions WHERE status != 'DONE' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActive(): BakeSession?

    @Query("SELECT * FROM bake_sessions WHERE status != 'DONE' ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<BakeSession>>

    @Query("SELECT COUNT(*) FROM bake_sessions WHERE status = 'DONE'")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM bake_sessions WHERE status = 'DONE'")
    suspend fun completedCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: BakeSession): Long

    @Update
    suspend fun update(session: BakeSession)

    @Query("UPDATE bake_sessions SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query("DELETE FROM bake_sessions WHERE status = 'DONE'")
    suspend fun clearCompleted()
}
