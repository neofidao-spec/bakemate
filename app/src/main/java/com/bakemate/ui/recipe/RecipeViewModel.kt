package com.bakemate.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bakemate.data.repository.RecipeRepository
import com.bakemate.domain.model.IngredientModel
import com.bakemate.domain.model.RecipeFormula
import com.bakemate.domain.model.StepModel
import com.bakemate.domain.usecase.ValidateRecipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeListUiState(
    val recipes: List<RecipeFormula> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false
)

data class RecipeDetailUiState(
    val recipe: RecipeFormula? = null,
    val isLoading: Boolean = true
)

data class RecipeFormUiState(
    val isVisible: Boolean = false,
    val editingId: Long? = null,
    val name: String = "",
    val description: String = "",
    val ingredients: List<IngredientModel> = listOf(
        IngredientModel(name = "Tepung", grams = 500.0, isFlour = true),
        IngredientModel(name = "Air", grams = 350.0)
    ),
    val steps: List<StepModel> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecipeViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _showFavoritesOnly = MutableStateFlow(false)
    private val _formState = MutableStateFlow(RecipeFormUiState())
    val formState: StateFlow<RecipeFormUiState> = _formState.asStateFlow()

    private val _detailRecipeId = MutableStateFlow<Long?>(null)

    val listState: StateFlow<RecipeListUiState> =
        combine(_searchQuery, _showFavoritesOnly) { query, favOnly -> query to favOnly }
            .flatMapLatest { (query, favOnly) ->
                val source = if (query.isBlank()) repository.observeAll() else repository.search(query)
                source.map { recipes ->
                    RecipeListUiState(
                        recipes = if (favOnly) recipes.filter { it.isFavorite } else recipes,
                        isLoading = false,
                        searchQuery = query,
                        showFavoritesOnly = favOnly
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeListUiState())

    val detailState: StateFlow<RecipeDetailUiState> =
        _detailRecipeId
            .flatMapLatest { id ->
                if (id == null) flowOf(RecipeDetailUiState(recipe = null, isLoading = false))
                else repository.observeById(id).map { formula ->
                    RecipeDetailUiState(recipe = formula, isLoading = false)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeDetailUiState())

    // ===== List actions =====
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoritesOnly() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun selectRecipe(id: Long) {
        _detailRecipeId.value = id
    }

    fun clearSelection() {
        _detailRecipeId.value = null
    }

    // ===== Form actions =====
    fun showAddForm() {
        _formState.value = RecipeFormUiState(
            isVisible = true,
            ingredients = listOf(
                IngredientModel(name = "Tepung", grams = 500.0, isFlour = true),
                IngredientModel(name = "Air", grams = 350.0)
            )
        )
    }

    fun showEditForm(formula: RecipeFormula) {
        _formState.value = RecipeFormUiState(
            isVisible = true,
            editingId = formula.id,
            name = formula.name,
            description = formula.description,
            ingredients = formula.ingredients,
            steps = formula.steps
        )
    }

    fun hideForm() {
        _formState.update { it.copy(isVisible = false, editingId = null, errorMessage = null) }
    }

    fun updateFormName(value: String) = _formState.update { it.copy(name = value) }
    fun updateFormDescription(value: String) = _formState.update { it.copy(description = value) }

    fun updateIngredient(index: Int, ingredient: IngredientModel) {
        _formState.update { state ->
            val list = state.ingredients.toMutableList()
            if (index in list.indices) list[index] = ingredient
            state.copy(ingredients = list)
        }
    }

    fun addIngredient() {
        _formState.update { it.copy(ingredients = it.ingredients + IngredientModel(name = "", grams = 0.0)) }
    }

    fun removeIngredient(index: Int) {
        _formState.update { state ->
            state.copy(ingredients = state.ingredients.filterIndexed { i, _ -> i != index })
        }
    }

    fun updateStep(index: Int, step: StepModel) {
        _formState.update { state ->
            val list = state.steps.toMutableList()
            if (index in list.indices) list[index] = step
            state.copy(steps = list)
        }
    }

    fun addStep() {
        _formState.update { it.copy(steps = it.steps + StepModel(text = "", minutes = 0)) }
    }

    fun removeStep(index: Int) {
        _formState.update { state ->
            state.copy(steps = state.steps.filterIndexed { i, _ -> i != index })
        }
    }

    fun saveRecipe() {
        val state = _formState.value
        val formula = RecipeFormula(
            id = state.editingId ?: 0L,
            name = state.name.trim(),
            description = state.description.trim(),
            ingredients = state.ingredients.filter { it.name.isNotBlank() },
            steps = state.steps.filter { it.text.isNotBlank() }
        )
        val error = ValidateRecipe.validate(formula)
        if (error != null) {
            _formState.update { it.copy(errorMessage = error) }
            return
        }

        viewModelScope.launch {
            repository.saveFormula(formula)
            _formState.update {
                it.copy(
                    isVisible = false,
                    editingId = null,
                    errorMessage = null,
                    successMessage = "Resep tersimpan: ${formula.name}"
                )
            }
        }
    }

    fun deleteRecipe(id: Long) {
        viewModelScope.launch {
            repository.deleteFormula(id)
            _formState.update { it.copy(successMessage = "Resep dihapus") }
            clearSelection()
        }
    }

    fun toggleFavorite(id: Long, current: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(id, !current)
        }
    }

    fun dismissSuccess() = _formState.update { it.copy(successMessage = null) }
}
