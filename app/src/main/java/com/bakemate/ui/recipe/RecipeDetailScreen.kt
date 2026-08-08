package com.bakemate.ui.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakemate.domain.model.RecipeFormula
import com.bakemate.ui.components.LoadingState
import com.bakemate.ui.components.SectionHeader
import com.bakemate.ui.components.StatCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: Long,
    onBack: () -> Unit,
    onEdit: (RecipeFormula) -> Unit,
    onStartBaking: (RecipeFormula) -> Unit,
    viewModel: RecipeViewModel = hiltViewModel()
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStartDialog by remember { mutableStateOf(false) }

    val recipe = state.recipe

    // Dialog konfirmasi mulai baking (preview tahap & durasi)
    if (showStartDialog && recipe != null) {
        val bakeSteps = recipe.steps.filter { it.minutes > 0 }
        val totalMinutes = bakeSteps.sumOf { it.minutes.toLong() }
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text("Mulai Baking?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${bakeSteps.size} tahap, total ±${formatMinutes(totalMinutes)}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Timer akan berjalan walau aplikasi ditutup, dan memberi notifikasi di tiap tahap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showStartDialog = false
                    onStartBaking(recipe)
                }) {
                    Text("Mulai")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe?.name ?: "Detail Resep") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (recipe != null) {
                        IconButton(onClick = { viewModel.toggleFavorite(recipe.id, recipe.isFavorite) }) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "Favorit",
                                tint = if (recipe.isFavorite) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )
                        }
                        IconButton(onClick = { onEdit(recipe) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Hapus")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
        } else if (recipe != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (recipe.description.isNotBlank()) {
                            Text(
                                text = recipe.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Hydration",
                                value = "${recipe.hydrationPercent}%",
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Total",
                                value = "${recipe.totalWeight.toInt()} g",
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Langkah",
                                value = "${recipe.stepCount}",
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = { showStartDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mulai Baking")
                    }
                }

                if (recipe.ingredients.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Formula")
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recipe.ingredients.forEach { ingredient ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = ingredient.name +
                                                if (ingredient.isFlour) " (tepung)" else "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = formatGrams(ingredient.grams),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                if (recipe.hydrationPercent > 0) {
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { (recipe.hydrationPercent / 100f).toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "Hydration ${recipe.hydrationPercent}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (recipe.steps.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Langkah")
                    }
                    itemsIndexed(recipe.steps) { index, step ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = step.text,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (step.minutes > 0) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "${step.minutes} menit",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showDeleteDialog && recipe != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Resep") },
            text = { Text("Yakin ingin menghapus '${recipe.name}'? Tindakan ini tidak bisa dibatalkan.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteRecipe(recipe.id)
                    onBack()
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

private fun formatGrams(grams: Double): String {
    return if (grams % 1.0 == 0.0) "${grams.toInt()} g" else "$grams g"
}

private fun formatMinutes(totalMinutes: Long): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours} jam ${minutes} menit"
        hours > 0 -> "${hours} jam"
        else -> "$minutes menit"
    }
}
