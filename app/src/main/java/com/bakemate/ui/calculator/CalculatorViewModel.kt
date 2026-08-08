package com.bakemate.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bakemate.domain.BakerMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class CalculatorUiState(
    val flourGrams: String = "500",
    val waterGrams: String = "350",
    val starterGrams: String = "100",
    val saltGrams: String = "10",
    val targetYield: String = "",
    val hydration: Double = 0.0,
    val totalWeight: Double = 0.0,
    val scaled: List<Double> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class CalculatorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    init {
        recalculate()
    }

    fun updateFlour(value: String) {
        _uiState.update { it.copy(flourGrams = value) }
        recalculate()
    }

    fun updateWater(value: String) {
        _uiState.update { it.copy(waterGrams = value) }
        recalculate()
    }

    fun updateStarter(value: String) {
        _uiState.update { it.copy(starterGrams = value) }
        recalculate()
    }

    fun updateSalt(value: String) {
        _uiState.update { it.copy(saltGrams = value) }
        recalculate()
    }

    fun updateTargetYield(value: String) {
        _uiState.update { it.copy(targetYield = value) }
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        val flour = state.flourGrams.toDoubleOrNull()
        val water = state.waterGrams.toDoubleOrNull()
        val starter = state.starterGrams.toDoubleOrNull()
        val salt = state.saltGrams.toDoubleOrNull()

        if (flour == null || water == null || starter == null || salt == null) {
            _uiState.update {
                it.copy(
                    hydration = 0.0,
                    totalWeight = 0.0,
                    scaled = emptyList(),
                    errorMessage = "Isi semua bahan dengan angka yang valid"
                )
            }
            return
        }

        if (flour <= 0) {
            _uiState.update {
                it.copy(
                    hydration = 0.0,
                    totalWeight = 0.0,
                    scaled = emptyList(),
                    errorMessage = "Tepung harus lebih dari 0"
                )
            }
            return
        }

        val hydration = BakerMath.hydrationPercent(flour, water, starter)
        val total = BakerMath.totalWeight(flour, water, starter, salt)

        val target = state.targetYield.toDoubleOrNull()
        val scaled = if (target != null && target > 0) {
            BakerMath.scaleToYield(flour, water, starter, salt, target) ?: emptyList()
        } else {
            emptyList()
        }

        _uiState.update {
            it.copy(
                hydration = hydration,
                totalWeight = total,
                scaled = scaled,
                errorMessage = null
            )
        }
    }

    fun clear() {
        _uiState.value = CalculatorUiState()
        recalculate()
    }
}
