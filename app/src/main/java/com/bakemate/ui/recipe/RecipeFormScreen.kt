package com.bakemate.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakemate.domain.model.IngredientModel
import com.bakemate.domain.model.StepModel
import com.bakemate.ui.components.NumberField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeFormScreen(
    onBack: () -> Unit,
    viewModel: RecipeViewModel = hiltViewModel()
) {
    val state by viewModel.formState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.editingId != null) "Edit Resep" else "Resep Baru") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== Info dasar =====
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateFormName,
                label = { Text("Nama Resep *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::updateFormDescription,
                label = { Text("Deskripsi (opsional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // ===== Bahan =====
            Text(
                text = "Bahan",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            state.ingredients.forEachIndexed { index, ingredient ->
                IngredientRow(
                    ingredient = ingredient,
                    onNameChange = { name ->
                        viewModel.updateIngredient(index, ingredient.copy(name = name))
                    },
                    onGramsChange = { grams ->
                        viewModel.updateIngredient(index, ingredient.copy(grams = grams))
                    },
                    onIsFlourChange = { isFlour ->
                        viewModel.updateIngredient(index, ingredient.copy(isFlour = isFlour))
                    },
                    onRemove = { viewModel.removeIngredient(index) }
                )
            }
            OutlinedButton(
                onClick = viewModel::addIngredient,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Tambah Bahan")
            }

            // ===== Langkah =====
            Text(
                text = "Langkah",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (state.steps.isEmpty()) {
                Text(
                    text = "Belum ada langkah. Tambahkan langkah pertama.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.steps.forEachIndexed { index, step ->
                StepRow(
                    index = index,
                    step = step,
                    onTextChange = { text ->
                        viewModel.updateStep(index, step.copy(text = text))
                    },
                    onMinutesChange = { minutes ->
                        viewModel.updateStep(index, step.copy(minutes = minutes))
                    },
                    onRemove = { viewModel.removeStep(index) }
                )
            }
            OutlinedButton(
                onClick = viewModel::addStep,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Tambah Langkah")
            }

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = viewModel::saveRecipe,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Resep")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun IngredientRow(
    ingredient: IngredientModel,
    onNameChange: (String) -> Unit,
    onGramsChange: (Double) -> Unit,
    onIsFlourChange: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = ingredient.name,
                    onValueChange = onNameChange,
                    label = { Text("Nama bahan") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Hapus bahan",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberField(
                    value = if (ingredient.grams % 1.0 == 0.0) ingredient.grams.toInt().toString()
                    else ingredient.grams.toString(),
                    onValueChange = { input ->
                        input.replace(',', '.').toDoubleOrNull()?.let(onGramsChange)
                    },
                    label = "Gram",
                    suffix = "g",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tepung",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Switch(
                        checked = ingredient.isFlour,
                        onCheckedChange = onIsFlourChange
                    )
                }
            }
        }
    }
}

@Composable
private fun StepRow(
    index: Int,
    step: StepModel,
    onTextChange: (String) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Langkah ${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Hapus langkah",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            OutlinedTextField(
                value = step.text,
                onValueChange = onTextChange,
                label = { Text("Instruksi") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = if (step.minutes > 0) step.minutes.toString() else "",
                    onValueChange = { input ->
                        input.filter { it.isDigit() }.toIntOrNull()?.let(onMinutesChange)
                    },
                    label = { Text("Durasi (menit)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Kosongkan jika tanpa timer",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
