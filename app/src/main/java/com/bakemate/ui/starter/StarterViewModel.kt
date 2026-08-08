package com.bakemate.ui.starter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bakemate.data.local.entity.StarterLog
import com.bakemate.data.repository.StarterLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StarterUiState(
    val logs: List<StarterLog> = emptyList(),
    val latest: StarterLog? = null,
    val isLoading: Boolean = true,
    val formName: String = "Starter Utama",
    val formRatio: String = "1:1:1",
    val formNote: String = "",
    val formActivity: Int = 3,
    val isFormVisible: Boolean = false,
    val successMessage: String? = null
)

@HiltViewModel
class StarterViewModel @Inject constructor(
    private val repository: StarterLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StarterUiState())
    val uiState: StateFlow<StarterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { logs ->
                _uiState.update {
                    it.copy(
                        logs = logs,
                        latest = logs.firstOrNull(),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateFormName(value: String) = _uiState.update { it.copy(formName = value) }
    fun updateFormRatio(value: String) = _uiState.update { it.copy(formRatio = value) }
    fun updateFormNote(value: String) = _uiState.update { it.copy(formNote = value) }
    fun updateFormActivity(value: Int) = _uiState.update { it.copy(formActivity = value) }

    fun toggleForm() = _uiState.update { it.copy(isFormVisible = !it.isFormVisible) }

    fun saveLog() {
        val state = _uiState.value
        val name = state.formName.trim().ifEmpty { "Starter Utama" }
        viewModelScope.launch {
            repository.addLog(
                StarterLog(
                    starterName = name,
                    feedingTime = System.currentTimeMillis(),
                    ratio = state.formRatio.trim(),
                    note = state.formNote.trim(),
                    activityLevel = state.formActivity
                )
            )
            _uiState.update {
                it.copy(
                    isFormVisible = false,
                    formNote = "",
                    successMessage = "Feeding tercatat: $name"
                )
            }
        }
    }

    fun deleteLog(log: StarterLog) {
        viewModelScope.launch {
            repository.deleteLog(log)
        }
    }

    fun dismissSuccess() = _uiState.update { it.copy(successMessage = null) }
}
