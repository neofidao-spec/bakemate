package com.bakemate.ui.timer

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bakemate.data.repository.BakeSessionRepository
import com.bakemate.domain.BakeStage
import com.bakemate.domain.DefaultStages
import com.bakemate.domain.timer.BakeTimerEngine
import com.bakemate.domain.timer.BakeSessionModel
import com.bakemate.domain.timer.StageSnapshot
import com.bakemate.permission.PermissionHelper
import com.bakemate.timer.TimerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimerStageUi(
    val name: String,
    val totalSeconds: Long,
    val remainingSeconds: Long,
    val isDone: Boolean = false
)

data class TimerUiState(
    val stages: List<TimerStageUi> = emptyList(),
    val currentIndex: Int = 0,
    val isBaking: Boolean = false,
    val isPaused: Boolean = false,
    val bakeMode: Boolean = false,
    val sessionName: String = "Timer Bebas",
    val editMode: Boolean = false,
    val needNotificationPermission: Boolean = false,
    val needExactAlarmPermission: Boolean = false,
    val finishedMessage: String? = null
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val sessionRepository: BakeSessionRepository,
    private val timerScheduler: TimerScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null

    init {
        restoreActiveSession()
    }

    // ===== Session restore =====
    private fun restoreActiveSession() {
        viewModelScope.launch {
            val session = sessionRepository.getActive()
            if (session != null) {
                val now = SystemClock.elapsedRealtime()
                val index = BakeTimerEngine.currentStageIndex(session.stageEnds, now)
                val finished = BakeTimerEngine.isFinished(session.stageEnds, now)
                _uiState.value = TimerUiState(
                    stages = session.stages.mapIndexed { i, s ->
                        TimerStageUi(
                            name = s.name,
                            totalSeconds = s.totalSeconds,
                            remainingSeconds = if (finished) 0L
                            else BakeTimerEngine.remainingMs(session.stageEnds, i, now) / 1000L,
                            isDone = i < index
                        )
                    },
                    currentIndex = index,
                    isBaking = !finished && session.status != BakeSessionModel.STATUS_PAUSED,
                    isPaused = session.status == BakeSessionModel.STATUS_PAUSED,
                    bakeMode = session.bakeMode,
                    sessionName = session.recipeName,
                    needNotificationPermission = !PermissionHelper.hasNotificationPermission(context),
                    needExactAlarmPermission = !PermissionHelper.hasExactAlarmPermission(context)
                )
                if (!finished && session.status != BakeSessionModel.STATUS_PAUSED) {
                    startTicking()
                }
            } else {
                resetToDefaults()
            }
        }
    }

    fun resetToDefaults() {
        tickJob?.cancel()
        _uiState.value = TimerUiState(
            stages = DefaultStages.list.map {
                TimerStageUi(name = it.name, totalSeconds = it.defaultMinutes * 60L, remainingSeconds = it.defaultMinutes * 60L)
            },
            sessionName = "Timer Bebas",
            needNotificationPermission = !PermissionHelper.hasNotificationPermission(context),
            needExactAlarmPermission = !PermissionHelper.hasExactAlarmPermission(context)
        )
    }

    /** Mulai baking dari resep tertentu (dipanggil dari detail resep / home). */
    fun startBakingFromRecipe(recipeId: Long?, recipeName: String, stages: List<StageSnapshot>) {
        viewModelScope.launch {
            sessionRepository.startSession(recipeId, recipeName, stages, _uiState.value.bakeMode)
            val session = sessionRepository.getActive() ?: return@launch
            val now = SystemClock.elapsedRealtime()
            scheduleAlarms(session.stages, session.stageEnds)
            _uiState.value = TimerUiState(
                stages = session.stages.mapIndexed { i, s ->
                    TimerStageUi(
                        name = s.name,
                        totalSeconds = s.totalSeconds,
                        remainingSeconds = BakeTimerEngine.remainingMs(session.stageEnds, i, now) / 1000L,
                        isDone = false
                    )
                },
                currentIndex = 0,
                isBaking = true,
                bakeMode = _uiState.value.bakeMode,
                sessionName = recipeName,
                needNotificationPermission = !PermissionHelper.hasNotificationPermission(context),
                needExactAlarmPermission = !PermissionHelper.hasExactAlarmPermission(context)
            )
            startTicking()
        }
    }

    // ===== Kontrol =====
    fun setStageDuration(index: Int, minutes: Int) {
        val safe = minutes.coerceIn(1, 720)
        _uiState.update { state ->
            val newStages = state.stages.toMutableList()
            val current = newStages.getOrNull(index) ?: return@update state
            newStages[index] = current.copy(
                totalSeconds = safe * 60L,
                remainingSeconds = safe * 60L
            )
            state.copy(stages = newStages, editMode = true)
        }
    }

    fun toggleEditMode() = _uiState.update { it.copy(editMode = !it.editMode) }

    fun toggleBakeMode() {
        val newValue = !_uiState.value.bakeMode
        _uiState.update { it.copy(bakeMode = newValue) }
        if (_uiState.value.isBaking || _uiState.value.isPaused) {
            viewModelScope.launch {
                sessionRepository.getActive()?.let { session ->
                    sessionRepository.updateSession(session.copy(bakeMode = newValue))
                }
            }
        }
    }

    fun startBaking() {
        val state = _uiState.value
        val stages = state.stages.filter { it.totalSeconds > 0 }
            .map { StageSnapshot(it.name, it.totalSeconds) }
        if (stages.isEmpty()) return
        startBakingFromRecipe(null, state.sessionName, stages)
    }

    fun pauseBaking() {
        val state = _uiState.value
        if (!state.isBaking) return
        tickJob?.cancel()
        viewModelScope.launch {
            sessionRepository.setStatus(BakeSessionModel.STATUS_PAUSED)
        }
        _uiState.update { it.copy(isBaking = false, isPaused = true) }
    }

    fun resumeBaking() {
        val state = _uiState.value
        if (!state.isPaused) return
        viewModelScope.launch {
            val session = sessionRepository.getActive() ?: return@launch
            val now = SystemClock.elapsedRealtime()
            // Geser jadwal: tahap aktif punya sisa waktu sesuai remainingSeconds
            val remaining = state.stages.getOrNull(state.currentIndex)?.remainingSeconds ?: 0L
            val newEnds = BakeTimerEngine.resumeWithRemaining(
                stageEnds = session.stageEnds,
                currentIndex = state.currentIndex,
                remainingSeconds = remaining,
                nowElapsedRealtime = now
            )
            sessionRepository.advanceStage(newEnds, state.currentIndex)
            sessionRepository.setStatus(BakeSessionModel.STATUS_ACTIVE)
            // Re-jadwal alarm
            timerScheduler.cancelAll(session.stages.size)
            scheduleAlarms(session.stages, newEnds)
            _uiState.update { it.copy(isBaking = true, isPaused = false) }
            startTicking()
        }
    }

    fun skipStage() {
        val state = _uiState.value
        if (!state.isBaking) return
        timerScheduler.cancelStage(state.currentIndex)
        viewModelScope.launch {
            val session = sessionRepository.getActive() ?: return@launch
            val now = SystemClock.elapsedRealtime()
            val newIndex = (state.currentIndex + 1).coerceAtMost(session.stageEnds.size - 1)
            val newEnds = session.stageEnds.toMutableList()
            newEnds[state.currentIndex] = now
            sessionRepository.advanceStage(newEnds, newIndex)
            if (BakeTimerEngine.isFinished(newEnds, now)) {
                sessionRepository.markDone()
                _uiState.update {
                    it.copy(isBaking = false, currentIndex = 0, finishedMessage = "Baking selesai. Bagus!")
                }
            } else {
                timerScheduler.scheduleStage(
                    newIndex,
                    session.stages.getOrNull(newIndex)?.name ?: "Tahap",
                    session.stages.getOrNull(newIndex)?.totalSeconds?.times(1000L) ?: 0L,
                    newEnds[newIndex]
                )
                startTicking()
            }
        }
    }

    fun stopBaking() {
        tickJob?.cancel()
        timerScheduler.cancelAll(_uiState.value.stages.size)
        viewModelScope.launch {
            sessionRepository.markDone()
            _uiState.update {
                it.copy(
                    isBaking = false,
                    isPaused = false,
                    currentIndex = 0,
                    finishedMessage = "Baking selesai. Bagus!"
                )
            }
        }
    }

    fun dismissFinished() = _uiState.update { it.copy(finishedMessage = null) }

    fun refreshPermissions() {
        _uiState.update {
            it.copy(
                needNotificationPermission = !PermissionHelper.hasNotificationPermission(context),
                needExactAlarmPermission = !PermissionHelper.hasExactAlarmPermission(context)
            )
        }
    }

    // ===== Internal =====
    private fun scheduleAlarms(stages: List<StageSnapshot>, ends: List<Long>) {
        stages.forEachIndexed { index, stage ->
            timerScheduler.scheduleStage(index, stage.name, stage.totalSeconds * 1000L, ends[index])
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val now = SystemClock.elapsedRealtime()
                val session = sessionRepository.getActive() ?: break
                if (BakeTimerEngine.isFinished(session.stageEnds, now)) {
                    sessionRepository.markDone()
                    _uiState.update {
                        it.copy(
                            isBaking = false,
                            isPaused = false,
                            currentIndex = 0,
                            finishedMessage = "Baking selesai. Bagus!"
                        )
                    }
                    break
                }
                val index = BakeTimerEngine.currentStageIndex(session.stageEnds, now)
                _uiState.update { ui ->
                    ui.copy(
                        currentIndex = index,
                        stages = session.stages.mapIndexed { i, s ->
                            TimerStageUi(
                                name = s.name,
                                totalSeconds = s.totalSeconds,
                                remainingSeconds = BakeTimerEngine.remainingMs(session.stageEnds, i, now) / 1000L,
                                isDone = i < index
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
    }
}
