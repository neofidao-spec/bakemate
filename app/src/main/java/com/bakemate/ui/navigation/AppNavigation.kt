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
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.LaunchedEffect
import com.bakemate.R
import com.bakemate.domain.model.RecipeFormula
import com.bakemate.domain.timer.StageSnapshot
import com.bakemate.ui.calculator.CalculatorScreen
import com.bakemate.ui.home.HomeScreen
import com.bakemate.ui.recipe.RecipeDetailScreen
import com.bakemate.ui.recipe.RecipeFormScreen
import com.bakemate.ui.recipe.RecipeListScreen
import com.bakemate.ui.recipe.RecipeViewModel
import com.bakemate.ui.settings.SettingsScreen
import com.bakemate.ui.starter.StarterScreen
import com.bakemate.ui.timer.TimerScreen
import com.bakemate.ui.timer.TimerViewModel

sealed class Screen(val route: String, val label: String, val iconRes: Int) {
    data object Home : Screen("home", "Beranda", R.drawable.ic_home)
    data object Recipes : Screen("recipes", "Resep", R.drawable.ic_recipe)
    data object Timer : Screen("timer", "Timer", R.drawable.ic_timer)
    data object Starter : Screen("starter", "Starter", R.drawable.ic_starter)
    data object Settings : Screen("settings", "Pengaturan", R.drawable.ic_settings)
}

private val bottomTabs = listOf(
    Screen.Home,
    Screen.Recipes,
    Screen.Timer,
    Screen.Starter,
    Screen.Settings
)

// Rute detail/form (bukan tab)
object Routes {
    const val RECIPE_DETAIL = "recipes/{recipeId}"
    const val RECIPE_FORM = "recipes/form?recipeId={recipeId}"
    const val CALCULATOR = "calculator"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // TimerViewModel dibagikan (activity-scoped) agar detail resep & layar Timer
    // memakai instance yang sama — state baking tetap sinkron antar layar.
    val timerViewModel: TimerViewModel = hiltViewModel()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val isTabRoute = currentDestination?.hierarchy?.any {
                bottomTabs.any { tab -> tab.route == it.route }
            } == true

            if (isTabRoute) {
                NavigationBar {
                    bottomTabs.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTab(navController, screen.route) },
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onOpenRecipes = { navigateToTab(navController, Screen.Recipes.route) },
                    onOpenTimer = { navigateToTab(navController, Screen.Timer.route) },
                    onOpenActiveBaking = { navigateToTab(navController, Screen.Timer.route) },
                    onOpenCalculator = {
                        navController.navigate(Routes.CALCULATOR) { launchSingleTop = true }
                    }
                )
            }
            composable(Screen.Recipes.route) {
                RecipeListScreen(
                    onRecipeClick = { id -> navController.navigate("recipes/$id") { launchSingleTop = true } },
                    onAddClick = {
                        navController.navigate(Routes.RECIPE_FORM) { launchSingleTop = true }
                    }
                )
            }
            composable(Screen.Timer.route) {
                TimerScreen(viewModel = timerViewModel)
            }
            composable(Screen.Starter.route) { StarterScreen() }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // ===== Non-tab routes =====
            composable(Routes.RECIPE_DETAIL) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId")?.toLongOrNull() ?: 0L
                RecipeDetailScreen(
                    recipeId = recipeId,
                    onBack = { navController.popBackStack() },
                    onEdit = { recipe: RecipeFormula ->
                        navController.navigate("recipes/form?recipeId=${recipe.id}") {
                            launchSingleTop = true
                        }
                    },
                    onStartBaking = { recipe: RecipeFormula ->
                        val stages = recipe.steps
                            .filter { it.minutes > 0 }
                            .map { step ->
                                StageSnapshot(
                                    name = step.text.take(40),
                                    totalSeconds = step.minutes * 60L
                                )
                            }
                        if (stages.isEmpty()) return@RecipeDetailScreen
                        // Mulai baking langsung dari detail resep (VM dibagikan)
                        timerViewModel.startBakingFromRecipe(recipe.id, recipe.name, stages)
                        navigateToTab(navController, Screen.Timer.route)
                    }
                )
            }
            composable(
                route = Routes.RECIPE_FORM,
                arguments = listOf(navArgument("recipeId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getLong("recipeId") ?: -1L
                val formViewModel: RecipeViewModel = hiltViewModel()
                LaunchedEffect(recipeId) {
                    if (recipeId > 0) {
                        formViewModel.loadRecipeForEdit(recipeId)
                    } else {
                        formViewModel.showAddForm()
                    }
                }
                RecipeFormScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.CALCULATOR) {
                CalculatorScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/** Navigasi antar tab tanpa menumpuk back stack (pola bottom-nav resmi). */
private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

