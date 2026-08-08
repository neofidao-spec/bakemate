package com.bakemate.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bakemate.domain.DefaultStages
import com.bakemate.timer.TimerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StageTimer(
    val name: String,
    val totalSeconds: Long,
    val remainingSeconds: Long,
    val isRunning: Boolean = false,
    val isDone: Boolean = false
)

data class TimerUiState(
    val stages: List<StageTimer> = emptyList(),
    val currentIndex: Int = 0,
    val isBaking: Boolean = false,
    val bakeMode: Boolean = false,
    val editMode: Boolean = false
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerScheduler: TimerScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null

    init {
        resetToDefaults()
    }

    fun resetToDefaults() {
        tickJob?.cancel()
        val stages = DefaultStages.list.map {
            StageTimer(name = it.name, totalSeconds = it.defaultMinutes * 60L, remainingSeconds = it.defaultMinutes * 60L)
        }
        _uiState.value = TimerUiState(stages = stages)
    }

    fun setStageDuration(index: Int, minutes: Int) {
        val safe = minutes.coerceIn(1, 720)
        _uiState.update { state ->
            val newStages = state.stages.toMutableList()
            val current = newStages[index]
            newStages[index] = current.copy(
                totalSeconds = safe * 60L,
                remainingSeconds = safe * 60L
            )
            state.copy(stages = newStages, editMode = true)
        }
    }

    fun toggleEditMode() {
        _uiState.update { it.copy(editMode = !it.editMode) }
    }

    fun startBaking() {
        tickJob?.cancel()
        val state = _uiState.value
        val now = System.currentTimeMillis()
        state.stages.forEachIndexed { index, stage ->
            val startOffset = state.stages.take(index).sumOf { it.totalSeconds } * 1000L
            val triggerAt = now + startOffset + stage.totalSeconds * 1000L
            timerScheduler.scheduleStage(index, stage.name, stage.totalSeconds * 1000L, triggerAt)
        }
        _uiState.update {
            it.copy(
                isBaking = true,
                editMode = false,
                currentIndex = 0,
                stages = it.stages.map { s -> s.copy(isRunning = false, isDone = false) }
            )
        }
        startTicking()
    }

    fun pauseResume() {
        val state = _uiState.value
        val current = state.stages.getOrNull(state.currentIndex) ?: return
        if (current.isRunning) {
            tickJob?.cancel()
            _uiState.update { it.copy(stages = it.stages.mapIndexed { i, s ->
                if (i == state.currentIndex) s.copy(isRunning = false) else s
            }) }
        } else {
            startTicking()
            _uiState.update { it.copy(stages = it.stages.mapIndexed { i, s ->
                if (i == state.currentIndex) s.copy(isRunning = true) else s
            }) }
        }
    }

    fun skipStage() {
        val state = _uiState.value
        timerScheduler.cancelStage(state.currentIndex)
        advanceStage()
    }

    fun stopBaking() {
        tickJob?.cancel()
        timerScheduler.cancelAll(_uiState.value.stages.size)
        _uiState.update {
            it.copy(
                isBaking = false,
                currentIndex = 0,
                stages = it.stages.map { s -> s.copy(isRunning = false, isDone = false) }
            )
        }
    }

    fun toggleBakeMode() {
        _uiState.update { it.copy(bakeMode = !it.bakeMode) }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value
                if (!state.isBaking) break
                val index = state.currentIndex
                val current = state.stages.getOrNull(index) ?: break
                val newRemaining = (current.remainingSeconds - 1).coerceAtLeast(0)
                val newStages = state.stages.toMutableList()

                if (newRemaining == 0L) {
                    newStages[index] = current.copy(
                        remainingSeconds = 0L,
                        isDone = true,
                        isRunning = false
                    )
                    _uiState.update { it.copy(stages = newStages) }
                    advanceStage()
                } else {
                    newStages[index] = current.copy(
                        remainingSeconds = newRemaining,
                        isRunning = true
                    )
                    _uiState.update { it.copy(stages = newStages) }
                }
            }
        }
    }

    private fun advanceStage() {
        val state = _uiState.value
        val next = state.currentIndex + 1
        if (next >= state.stages.size) {
            tickJob?.cancel()
            timerScheduler.cancelAll(state.stages.size)
            _uiState.update {
                it.copy(
                    isBaking = false,
                    currentIndex = 0,
                    stages = it.stages.map { s -> s.copy(isRunning = false) }
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = next,
                    stages = it.stages.mapIndexed { i, s ->
                        if (i == next) s.copy(isRunning = true) else s
                    }
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
    }
}
