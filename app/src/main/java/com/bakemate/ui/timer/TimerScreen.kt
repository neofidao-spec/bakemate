package com.bakemate.ui.timer

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.view.WindowManager
import androidx.activity.ComponentActivity

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DisposableEffect(state.bakeMode) {
        val activity = context as? ComponentActivity
        if (state.bakeMode) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Timer Baking",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (state.isBaking) "Baking berjalan..." else "Atur durasi tiap tahap, lalu mulai",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val current = state.stages.getOrNull(state.currentIndex)
                    Text(
                        text = current?.name ?: "-",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = formatDuration(current?.remainingSeconds ?: 0L),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (state.isBaking) {
                        Text(
                            text = "Tahap ${state.currentIndex + 1} dari ${state.stages.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (state.isBaking) viewModel.pauseResume() else viewModel.startBaking()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (state.isBaking) {
                            if (state.stages.getOrNull(state.currentIndex)?.isRunning == true) "Jeda" else "Lanjut"
                        } else {
                            "Mulai Baking"
                        }
                    )
                }
                if (state.isBaking) {
                    OutlinedButton(
                        onClick = viewModel::skipStage,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lewati")
                    }
                } else {
                    OutlinedButton(
                        onClick = viewModel::toggleEditMode,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Edit Durasi")
                    }
                }
            }
        }

        if (state.isBaking) {
            item {
                OutlinedButton(
                    onClick = viewModel::stopBaking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Hentikan Baking")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bake Mode", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Layar tetap menyala saat baking",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.bakeMode,
                    onCheckedChange = { viewModel.toggleBakeMode() }
                )
            }
        }

        item {
            Text(
                text = "Tahapan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        itemsIndexed(state.stages) { index, stage ->
            StageRow(
                stage = stage,
                isCurrent = index == state.currentIndex && state.isBaking,
                editMode = state.editMode && !state.isBaking,
                onEdit = { minutes -> viewModel.setStageDuration(index, minutes) }
            )
        }
    }
}

@Composable
private fun StageRow(
    stage: StageTimer,
    isCurrent: Boolean,
    editMode: Boolean,
    onEdit: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stage.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (stage.isDone) "Selesai" else formatDuration(stage.remainingSeconds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (stage.isDone) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (editMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(30, 60, 120, 180, 240).forEach { minutes ->
                        OutlinedButton(
                            onClick = { onEdit(minutes) },
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text("${minutes / 60}j" + if (minutes % 60 != 0) "${minutes % 60}m" else "")
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
