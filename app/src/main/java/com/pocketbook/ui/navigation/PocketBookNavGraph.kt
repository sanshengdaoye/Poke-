package com.pocketbook.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pocketbook.ui.screens.*

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : Screen("dashboard", "首页", Icons.Filled.Home, Icons.Outlined.Home)
    data object Flow : Screen("flow", "流水", Icons.Filled.List, Icons.Outlined.List)
    data object Stats : Screen("stats", "统计", Icons.Filled.BarChart, Icons.Outlined.BarChart)
    data object Budget : Screen("budget", "预算", Icons.Filled.PieChart, Icons.Outlined.PieChart)
    data object Me : Screen("me", "我的", Icons.Filled.Person, Icons.Outlined.Person)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Flow,
    Screen.Stats,
    Screen.Budget,
    Screen.Me
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocketBookNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            val currentRoute = currentDestination?.route
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Dashboard.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onAddClick = { navController.navigate("record") },
                    onViewTransactions = { navController.navigate(Screen.Flow.route) },
                    onViewStats = { navController.navigate(Screen.Stats.route) },
                    onViewBudget = { navController.navigate(Screen.Budget.route) },
                    onViewAccounts = { navController.navigate("accounts") },
                    onViewInsights = { navController.navigate("insights") },
                    onSettings = { navController.navigate("settings") }
                )
            }
            composable(Screen.Flow.route) {
                FlowScreen(
                    onAddClick = { navController.navigate("record") }
                )
            }
            composable(Screen.Stats.route) {
                StatsScreen()
            }
            composable(Screen.Budget.route) {
                BudgetScreen()
            }
            composable(Screen.Me.route) {
                MeScreen()
            }
            composable("record") {
                RecordScreen(
                    onSaveComplete = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable("accounts") {
                AccountScreen()
            }
            composable("insights") {
                InsightScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
