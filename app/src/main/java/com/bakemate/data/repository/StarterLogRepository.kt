package com.bakemate.data.repository

import com.bakemate.data.local.dao.StarterLogDao
import com.bakemate.data.local.entity.StarterLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StarterLogRepository @Inject constructor(
    private val dao: StarterLogDao
) {
    fun observeAll(): Flow<List<StarterLog>> = dao.observeAll()

    fun observeLatest(): Flow<StarterLog?> = dao.observeLatest()

    suspend fun getLatest(): StarterLog? = dao.getLatest()

    suspend fun addLog(log: StarterLog): Long = dao.insert(log)

    suspend fun deleteLog(log: StarterLog) = dao.delete(log)

    suspend fun count(): Int = dao.count()
}
