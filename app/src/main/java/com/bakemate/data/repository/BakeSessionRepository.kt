package com.bakemate.data.repository

import android.os.SystemClock
import com.bakemate.data.local.dao.BakeSessionDao
import com.bakemate.data.local.entity.BakeSession
import com.bakemate.domain.timer.BakeSessionModel
import com.bakemate.domain.timer.BakeTimerEngine
import com.bakemate.domain.timer.StageSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BakeSessionRepository @Inject constructor(
    private val dao: BakeSessionDao
) {

    fun observeActive(): Flow<BakeSessionModel?> = dao.observeActive().map { it?.toModel() }

    suspend fun getActive(): BakeSessionModel? = dao.getActive()?.toModel()

    fun observeCompletedCount(): Flow<Int> = dao.observeCompletedCount()

    suspend fun startSession(
        recipeId: Long?,
        recipeName: String,
        stages: List<StageSnapshot>,
        bakeMode: Boolean
    ): Long {
        val startElapsed = SystemClock.elapsedRealtime()
        val ends = BakeTimerEngine.buildStageEnds(stages, startElapsed)
        val session = BakeSession(
            recipeId = recipeId,
            recipeName = recipeName,
            stagesJson = encodeStages(stages),
            stageEndsJson = encodeEnds(ends),
            currentStageIndex = 0,
            startedAt = System.currentTimeMillis(),
            bakeMode = bakeMode,
            status = BakeSession.STATUS_ACTIVE
        )
        return dao.insert(session)
    }

    suspend fun updateSession(model: BakeSessionModel) {
        dao.getActive()?.let { existing ->
            dao.update(
                existing.copy(
                    recipeId = model.recipeId,
                    recipeName = model.recipeName,
                    stagesJson = encodeStages(model.stages),
                    stageEndsJson = encodeEnds(model.stageEnds),
                    currentStageIndex = model.currentStageIndex,
                    bakeMode = model.bakeMode,
                    status = model.status
                )
            )
        }
    }

    suspend fun markDone() {
        dao.getActive()?.let { dao.setStatus(it.id, BakeSession.STATUS_DONE) }
    }

    /** Advance ke tahap berikutnya / geser jadwal (skip, pause-freeze). */
    suspend fun advanceStage(stageEnds: List<Long>, currentIndex: Int) {
        dao.getActive()?.let { existing ->
            dao.update(
                existing.copy(
                    stageEndsJson = encodeEnds(stageEnds),
                    currentStageIndex = currentIndex
                )
            )
        }
    }

    /** Set status sesi aktif (PAUSED / ACTIVE). */
    suspend fun setStatus(status: String) {
        dao.getActive()?.let { dao.setStatus(it.id, status) }
    }

    suspend fun clearCompleted() = dao.clearCompleted()

    // ===== JSON helpers =====
    private fun encodeStages(stages: List<StageSnapshot>): String {
        val arr = JSONArray()
        stages.forEach { stage ->
            arr.put(JSONObject().put("name", stage.name).put("totalSeconds", stage.totalSeconds))
        }
        return arr.toString()
    }

    private fun encodeEnds(ends: List<Long>): String {
        val arr = JSONArray()
        ends.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun decodeStages(json: String): List<StageSnapshot> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                StageSnapshot(
                    name = obj.getString("name"),
                    totalSeconds = obj.getLong("totalSeconds")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun decodeEnds(json: String): List<Long> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getLong(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun BakeSession.toModel(): BakeSessionModel = BakeSessionModel(
        id = id,
        recipeId = recipeId,
        recipeName = recipeName,
        stages = decodeStages(stagesJson),
        currentStageIndex = currentStageIndex,
        startedAt = startedAt,
        bakeMode = bakeMode,
        status = status,
        stageEnds = decodeEnds(stageEndsJson)
    )
}
