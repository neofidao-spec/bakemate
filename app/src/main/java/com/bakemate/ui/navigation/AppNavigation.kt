package com.bakemate.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bakemate.R
import com.bakemate.ui.calculator.CalculatorScreen
import com.bakemate.ui.home.HomeScreen
import com.bakemate.ui.recipe.RecipeScreen
import com.bakemate.ui.settings.SettingsScreen
import com.bakemate.ui.starter.StarterScreen
import com.bakemate.ui.timer.TimerScreen

sealed class Screen(val route: String, val label: String, val iconRes: Int) {
    data object Home : Screen("home", "Beranda", R.drawable.ic_home)
    data object Calculator : Screen("calculator", "Kalkulator", R.drawable.ic_calculator)
    data object Timer : Screen("timer", "Timer", R.drawable.ic_timer)
    data object Starter : Screen("starter", "Starter", R.drawable.ic_starter)
    data object Settings : Screen("settings", "Pengaturan", R.drawable.ic_settings)
}

private val bottomTabs = listOf(
    Screen.Home,
    Screen.Calculator,
    Screen.Timer,
    Screen.Starter,
    Screen.Settings
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            NavigationBar {
                bottomTabs.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(screen.iconRes),
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Calculator.route) { CalculatorScreen() }
            composable(Screen.Timer.route) { TimerScreen() }
            composable(Screen.Starter.route) { StarterScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
