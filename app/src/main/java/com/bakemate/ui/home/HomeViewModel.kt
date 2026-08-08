package com.bakemate.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bakemate.data.repository.BakeSessionRepository
import com.bakemate.data.repository.RecipeRepository
import com.bakemate.domain.timer.BakeSessionModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val activeSession: BakeSessionModel? = null,
    val recipeCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessionRepository: BakeSessionRepository,
    recipeRepository: RecipeRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(
            sessionRepository.observeActive(),
            recipeRepository.observeAll()
        ) { session, recipes ->
            HomeUiState(
                activeSession = session,
                recipeCount = recipes.size
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
