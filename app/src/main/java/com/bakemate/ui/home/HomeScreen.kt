package com.bakemate.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bakemate.R
import com.bakemate.ui.components.StatCard
import com.bakemate.ui.navigation.Screen
import com.bakemate.ui.recipe.RecipeViewModel
import com.bakemate.ui.starter.StarterViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    recipeViewModel: RecipeViewModel = hiltViewModel(),
    starterViewModel: StarterViewModel = hiltViewModel()
) {
    val recipeState by recipeViewModel.uiState.collectAsStateWithLifecycle()
    val starterState by starterViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "BakeMate",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Pendamping baking harian Anda",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Resep Tersimpan",
                value = recipeState.recipes.size.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Catatan Starter",
                value = starterState.logs.size.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Mulai Cepat",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        QuickActionCard(
            iconRes = R.drawable.ic_calculator,
            title = "Kalkulator Baker",
            subtitle = "Hydration & scaling resep",
            onClick = { navController.navigate(Screen.Calculator.route) }
        )
        QuickActionCard(
            iconRes = R.drawable.ic_timer,
            title = "Timer Multi-Tahap",
            subtitle = "Autolyse sampai panggang",
            onClick = { navController.navigate(Screen.Timer.route) }
        )
        QuickActionCard(
            iconRes = R.drawable.ic_starter,
            title = "Jurnal Starter",
            subtitle = "Catat feeding & aktivitas",
            onClick = { navController.navigate(Screen.Starter.route) }
        )
    }
}

@Composable
private fun QuickActionCard(
    iconRes: Int,
    title: String,
    subtitle: String,
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
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column {
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
}
