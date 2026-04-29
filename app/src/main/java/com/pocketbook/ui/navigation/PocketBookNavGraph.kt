package com.pocketbook.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pocketbook.ui.screens.*

@Composable
fun PocketBookNavGraph(
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Dashboard.route) { 
                DashboardScreen(
                    onNavigateToRecord = {
                        navController.navigate(BottomNavItem.Record.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToTransactions = {
                        navController.navigate(BottomNavItem.Flow.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToStats = {
                        navController.navigate(BottomNavItem.Stats.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToBudget = {
                        navController.navigate(BottomNavItem.Budget.route) {
                            launchSingleTop = true
                        }
                    }
                ) 
            }
            composable(BottomNavItem.Flow.route) { FlowScreen() }
            composable(BottomNavItem.Record.route) { 
                RecordScreen(
                    onSaveComplete = {
                        navController.navigate(BottomNavItem.Dashboard.route) {
                            popUpTo(BottomNavItem.Dashboard.route) { inclusive = true }
                        }
                    }
                ) 
            }
            composable(BottomNavItem.Stats.route) { StatsScreen() }
            composable(BottomNavItem.Budget.route) { BudgetScreen() }
            composable(BottomNavItem.Me.route) { MeScreen() }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        BottomNavItem.items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
