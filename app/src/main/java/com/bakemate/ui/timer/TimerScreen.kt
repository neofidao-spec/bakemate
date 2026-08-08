package com.bakemate.ui.timer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showNotifPermissionDialog by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    var showStopConfirm by remember { mutableStateOf(false) }

    // Dialog permission
    if (showNotifPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showNotifPermissionDialog = false },
            title = { Text("Izin Notifikasi") },
            text = { Text("BakeMate memerlukan izin notifikasi agar timer memberi tahu Anda saat tahap selesai, bahkan saat aplikasi ditutup.") },
            confirmButton = {
                TextButton(onClick = {
                    showNotifPermissionDialog = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Kirim request via activity result di MainActivity
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    }
                }) {
                    Text("Buka Pengaturan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotifPermissionDialog = false }) {
                    Text("Nanti")
                }
            }
        )
    }

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text("Izin Alarm Presisi") },
            text = { Text("Agar timer berjalan tepat waktu saat layar mati, aktifkan izin 'Alarm & pengingat' untuk BakeMate di pengaturan sistem.") },
            confirmButton = {
                TextButton(onClick = {
                    showExactAlarmDialog = false
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.parse("package:${context.packageName}"))
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // fallback ke settings umum
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }) {
                    Text("Buka Pengaturan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmDialog = false }) {
                    Text("Nanti")
                }
            }
        )
    }

    // Konfirmasi selesaikan baking
    if (showStopConfirm) {
        AlertDialog(
            onDismissRequest = { showStopConfirm = false },
            title = { Text("Selesaikan baking?") },
            text = { Text("Sesi baking akan ditandai selesai dan tidak dapat dilanjutkan.") },
            confirmButton = {
                TextButton(onClick = {
                    showStopConfirm = false
                    viewModel.stopBaking()
                }) {
                    Text("Selesai")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Banner izin
    if (state.needNotificationPermission || state.needExactAlarmPermission) {
        PermissionBanner(
            needNotif = state.needNotificationPermission,
            needAlarm = state.needExactAlarmPermission,
            onNotifClick = { showNotifPermissionDialog = true },
            onAlarmClick = { showExactAlarmDialog = true }
        )
    }

    // Pesan selesai
    state.finishedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissFinished,
            title = { Text("Selesai") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissFinished) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = state.sessionName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Mode Baking (layar tetap menyala)",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = state.bakeMode,
                onCheckedChange = { viewModel.toggleBakeMode() }
            )
        }

        Spacer(Modifier.height(12.dp))

        // Stage utama (aktif atau dijeda)
        val currentStage = state.stages.getOrNull(state.currentIndex)
        if (currentStage != null && (state.isBaking || state.isPaused)) {
            ActiveStageCard(
                stage = currentStage,
                index = state.currentIndex,
                total = state.stages.size,
                isPaused = state.isPaused
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.isPaused) {
                    Button(
                        onClick = viewModel::resumeBaking,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Lanjutkan")
                    }
                    OutlinedButton(
                        onClick = { showStopConfirm = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(com.bakemate.R.drawable.ic_stop),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Selesai")
                    }
                } else {
                    FilledTonalButton(
                        onClick = viewModel::pauseBaking,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(com.bakemate.R.drawable.ic_pause),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Jeda")
                    }
                    OutlinedButton(
                        onClick = viewModel::skipStage,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lewati")
                    }
                    OutlinedButton(
                        onClick = { showStopConfirm = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(com.bakemate.R.drawable.ic_stop),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Selesai")
                    }
                }
            }
        } else if (!state.isBaking && !state.isPaused) {
            // Belum mulai
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Atur tahap, lalu mulai timer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Timer akan terus berjalan walau aplikasi ditutup — notifikasi memberi tahu saat tiap tahap selesai.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = viewModel::startBaking,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Mulai Timer")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Daftar semua tahap
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(state.stages) { index, stage ->
                StageRow(
                    stage = stage,
                    index = index,
                    isCurrent = index == state.currentIndex && (state.isBaking || state.isPaused),
                    editMode = state.editMode && !state.isBaking && !state.isPaused,
                    onDurationChange = { minutes -> viewModel.setStageDuration(index, minutes) }
                )
            }
        }
    }
}

@Composable
private fun PermissionBanner(
    needNotif: Boolean,
    needAlarm: Boolean,
    onNotifClick: () -> Unit,
    onAlarmClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Aktifkan izin agar timer berjalan sempurna",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (needNotif) {
                TextButton(onClick = onNotifClick) {
                    Text("Izin notifikasi belum aktif — buka pengaturan")
                }
            }
            if (needAlarm) {
                TextButton(onClick = onAlarmClick) {
                    Text("Izin alarm presisi belum aktif — buka pengaturan")
                }
            }
        }
    }
}

@Composable
private fun ActiveStageCard(
    stage: TimerStageUi,
    index: Int,
    total: Int,
    isPaused: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tahap ${index + 1} dari $total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stage.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatDuration(stage.remainingSeconds),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (isPaused) {
                Text(
                    text = "Dijeda",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(12.dp))
            val progress = if (stage.totalSeconds > 0) {
                (stage.remainingSeconds.toFloat() / stage.totalSeconds.toFloat()).coerceIn(0f, 1f)
            } else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StageRow(
    stage: TimerStageUi,
    index: Int,
    isCurrent: Boolean,
    editMode: Boolean,
    onDurationChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
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
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDuration(stage.remainingSeconds),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (stage.isDone) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (stage.isDone) {
                Text(
                    text = "Selesai",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (editMode) {
                DurationEditor(
                    minutes = (stage.totalSeconds / 60).toInt(),
                    onValueChange = onDurationChange
                )
            }
        }
    }
}

@Composable
private fun DurationEditor(
    minutes: Int,
    onValueChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = { onValueChange((minutes - 5).coerceAtLeast(1)) },
            modifier = Modifier.width(48.dp)
        ) {
            Text("-")
        }
        Text(
            text = "$minutes",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        OutlinedButton(
            onClick = { onValueChange(minutes + 5) },
            modifier = Modifier.width(48.dp)
        ) {
            Text("+")
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
