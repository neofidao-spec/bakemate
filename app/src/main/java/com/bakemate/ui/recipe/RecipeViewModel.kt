package com.bakemate.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bakemate.data.local.entity.Recipe
import com.bakemate.data.repository.RecipeRepository
import com.bakemate.domain.BakerMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeUiState(
    val recipes: List<Recipe> = emptyList(),
    val isLoading: Boolean = true,
    val formName: String = "",
    val formDescription: String = "",
    val formFlour: String = "500",
    val formWater: String = "350",
    val formStarter: String = "100",
    val formSalt: String = "10",
    val formSteps: String = "",
    val isFormVisible: Boolean = false,
    val editingId: Long? = null,
    val successMessage: String? = null
)

@HiltViewModel
class RecipeViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeUiState())
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { recipes ->
                _uiState.update { it.copy(recipes = recipes, isLoading = false) }
            }
        }
    }

    fun updateFormName(value: String) = _uiState.update { it.copy(formName = value) }
    fun updateFormDescription(value: String) = _uiState.update { it.copy(formDescription = value) }
    fun updateFormFlour(value: String) = _uiState.update { it.copy(formFlour = value) }
    fun updateFormWater(value: String) = _uiState.update { it.copy(formWater = value) }
    fun updateFormStarter(value: String) = _uiState.update { it.copy(formStarter = value) }
    fun updateFormSalt(value: String) = _uiState.update { it.copy(formSalt = value) }
    fun updateFormSteps(value: String) = _uiState.update { it.copy(formSteps = value) }

    fun showAddForm() = _uiState.update {
        it.copy(
            isFormVisible = true,
            editingId = null,
            formName = "",
            formDescription = "",
            formFlour = "500",
            formWater = "350",
            formStarter = "100",
            formSalt = "10",
            formSteps = ""
        )
    }

    fun showEditForm(recipe: Recipe) = _uiState.update {
        it.copy(
            isFormVisible = true,
            editingId = recipe.id,
            formName = recipe.name,
            formDescription = recipe.description,
            formFlour = recipe.totalFlourGrams.toInt().toString(),
            formWater = recipe.totalWaterGrams.toInt().toString(),
            formStarter = recipe.totalStarterGrams.toInt().toString(),
            formSalt = recipe.totalSaltGrams.toInt().toString(),
            formSteps = recipe.steps
        )
    }

    fun hideForm() = _uiState.update { it.copy(isFormVisible = false, editingId = null) }

    fun saveRecipe() {
        val state = _uiState.value
        val name = state.formName.trim()
        val flour = state.formFlour.toDoubleOrNull() ?: 0.0
        val water = state.formWater.toDoubleOrNull() ?: 0.0
        val starter = state.formStarter.toDoubleOrNull() ?: 0.0
        val salt = state.formSalt.toDoubleOrNull() ?: 0.0

        if (name.isEmpty() || flour <= 0) {
            _uiState.update { it.copy(successMessage = "Nama dan tepung wajib diisi") }
            return
        }

        val hydration = BakerMath.hydrationPercent(flour, water, starter)
        val editingId = state.editingId

        viewModelScope.launch {
            if (editingId != null) {
                val existing = repository.getById(editingId)
                if (existing != null) {
                    repository.updateRecipe(
                        existing.copy(
                            name = name,
                            description = state.formDescription.trim(),
                            totalFlourGrams = flour,
                            totalWaterGrams = water,
                            totalStarterGrams = starter,
                            totalSaltGrams = salt,
                            hydrationPercent = hydration,
                            steps = state.formSteps.trim()
                        )
                    )
                }
            } else {
                repository.addRecipe(
                    Recipe(
                        name = name,
                        description = state.formDescription.trim(),
                        totalFlourGrams = flour,
                        totalWaterGrams = water,
                        totalStarterGrams = starter,
                        totalSaltGrams = salt,
                        hydrationPercent = hydration,
                        steps = state.formSteps.trim()
                    )
                )
            }
            _uiState.update {
                it.copy(
                    isFormVisible = false,
                    editingId = null,
                    successMessage = "Resep tersimpan: $name"
                )
            }
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.deleteRecipe(recipe)
            _uiState.update { it.copy(successMessage = "Resep dihapus") }
        }
    }

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            repository.setFavorite(recipe.id, !recipe.isFavorite)
        }
    }

    fun dismissSuccess() = _uiState.update { it.copy(successMessage = null) }
}
