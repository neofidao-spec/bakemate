package com.bakemate.ui.starter

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakemate.data.local.entity.StarterLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StarterScreen(
    viewModel: StarterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Jurnal Starter",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Catat feeding, rasio, dan aktivitas starter Anda.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        state.latest?.let { latest ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Terakhir Feeding",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = latest.starterName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                        Text(
                            text = formatTime(latest.feedingTime),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (latest.ratio.isNotBlank()) {
                            Text(
                                text = "Rasio: ${latest.ratio}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        item {
            if (state.isFormVisible) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Catat Feeding Baru",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = state.formName,
                            onValueChange = viewModel::updateFormName,
                            label = { Text("Nama Starter") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.formRatio,
                            onValueChange = viewModel::updateFormRatio,
                            label = { Text("Rasio (mis. 1:1:1)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.formNote,
                            onValueChange = viewModel::updateFormNote,
                            label = { Text("Catatan") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Aktivitas: ${state.formActivity}/5",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = state.formActivity.toFloat(),
                            onValueChange = { viewModel.updateFormActivity(it.toInt()) },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = viewModel::saveLog,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Simpan")
                            }
                            OutlinedButton(
                                onClick = viewModel::toggleForm,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Batal")
                            }
                        }
                    }
                }
            } else {
                Button(
                    onClick = viewModel::toggleForm,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Catat Feeding")
                }
            }
        }

        state.successMessage?.let { message ->
            item {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            Text(
                text = "Riwayat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
        }

        if (state.logs.isEmpty()) {
            item {
                Text(
                    text = "Belum ada catatan. Mulai catat feeding pertama Anda!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.logs, key = { it.id }) { log ->
                StarterLogCard(log = log, onDelete = { viewModel.deleteLog(log) })
            }
        }
    }
}

@Composable
private fun StarterLogCard(
    log: StarterLog,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.starterName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                Text(
                    text = formatTime(log.feedingTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (log.ratio.isNotBlank()) {
                    Text(
                        text = "Rasio: ${log.ratio}  |  Aktivitas: ${log.activityLevel}/5",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (log.note.isNotBlank()) {
                    Text(
                        text = log.note,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Text(
                text = "Hapus",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .clickable { onDelete() }
                    .padding(4.dp)
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val format = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("id", "ID"))
    return format.format(Date(timestamp))
}
