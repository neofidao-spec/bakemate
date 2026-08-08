package com.bakemate.ui.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakemate.domain.model.RecipeFormula
import com.bakemate.ui.components.EmptyState

@Composable
fun RecipeListScreen(
    onRecipeClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    viewModel: RecipeViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah Resep")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Resep",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Formula roti Anda, lengkap dengan bahan & langkah.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Cari resep...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !state.showFavoritesOnly,
                    onClick = { if (state.showFavoritesOnly) viewModel.toggleFavoritesOnly() },
                    label = { Text("Semua") }
                )
                FilterChip(
                    selected = state.showFavoritesOnly,
                    onClick = { if (!state.showFavoritesOnly) viewModel.toggleFavoritesOnly() },
                    label = { Text("Favorit") }
                )
            }

            androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))

            if (state.recipes.isEmpty()) {
                EmptyState(
                    title = if (state.searchQuery.isNotBlank()) "Tidak ada hasil" else "Belum ada resep",
                    description = if (state.searchQuery.isNotBlank()) {
                        "Tidak ditemukan resep dengan kata '${state.searchQuery}'"
                    } else {
                        "Tambahkan resep pertama Anda untuk mulai menyimpan formula."
                    },
                    actionLabel = if (state.searchQuery.isBlank()) "Tambah Resep" else null,
                    onAction = if (state.searchQuery.isBlank()) onAddClick else null
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.recipes, key = { it.id }) { recipe ->
                        RecipeListItem(
                            recipe = recipe,
                            onClick = { onRecipeClick(recipe.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeListItem(
    recipe: RecipeFormula,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (recipe.isFavorite) {
                        Text(
                            text = "★ ",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                androidx.compose.foundation.layout.Spacer(Modifier.padding(2.dp))
                val hydration = recipe.hydrationPercent
                Text(
                    text = if (hydration > 0) {
                        "Hydration ${hydration}%  ·  ${recipe.stepCount} langkah"
                    } else {
                        "${recipe.stepCount} langkah"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${recipe.totalWeight.toInt()} g",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
