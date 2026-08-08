package com.bakemate.ui.home

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.bakemate.domain.timer.BakeTimerEngine
import com.bakemate.ui.components.EmptyState
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onOpenRecipes: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenActiveBaking: () -> Unit,
    onOpenCalculator: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Tick hanya saat ada sesi baking aktif — hindari recompose terus-menerus
    var tick by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0L) }
    val activeSession = state.activeSession
    val hasActiveSession = activeSession != null &&
        activeSession.status != "PAUSED" &&
        !BakeTimerEngine.isFinished(
            activeSession.stageEnds,
            SystemClock.elapsedRealtime()
        )
    LaunchedEffect(state.activeSession?.id) {
        if (hasActiveSession) {
            while (true) {
                tick = SystemClock.elapsedRealtime()
                delay(1000)
            }
        } else {
            tick = SystemClock.elapsedRealtime()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "BakeMate",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Teman memanggang roti Anda",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Kartu baking aktif
        val session = state.activeSession
        if (session != null) {
            item {
                val now = tick
                val index = BakeTimerEngine.currentStageIndex(session.stageEnds, now)
                val finished = BakeTimerEngine.isFinished(session.stageEnds, now)
                if (!finished && session.status != "PAUSED") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Baking sedang berjalan",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = session.recipeName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            val stage = session.stages.getOrNull(index)
                            Text(
                                text = "Tahap ${index + 1}/${session.stages.size}: " +
                                    (stage?.name ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            val remaining = stage?.let {
                                BakeTimerEngine.remainingMs(session.stageEnds, index, now) / 1000L
                            } ?: 0L
                            Text(
                                text = formatDuration(remaining),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            val total = session.totalDurationSeconds()
                            val elapsed = total - remaining
                            LinearProgressIndicator(
                                progress = {
                                    (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = onOpenActiveBaking,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.height(0.dp))
                                Text("Buka Timer")
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Resep",
                    subtitle = "${state.recipeCount} resep",
                    icon = {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(com.bakemate.R.drawable.ic_book),
                            contentDescription = null
                        )
                    },
                    onClick = onOpenRecipes,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Timer",
                    subtitle = "Baking bebas",
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    onClick = onOpenTimer,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Kalkulator",
                    subtitle = "Hydration & scaling",
                    icon = {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(com.bakemate.R.drawable.ic_calculator),
                            contentDescription = null
                        )
                    },
                    onClick = onOpenCalculator,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (state.recipeCount == 0) {
            item {
                EmptyState(
                    icon = Icons.Filled.PlayArrow,
                    title = "Mulai dengan resep",
                    description = "Tambahkan resep pertama Anda, atau langsung gunakan timer bebas untuk memanggang.",
                    actionLabel = "Tambah Resep",
                    onAction = onOpenRecipes
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            icon()
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
