package com.pocketbook.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Flow : BottomNavItem(
        route = "flow",
        title = "流水",
        selectedIcon = Icons.Filled.List,
        unselectedIcon = Icons.Outlined.List
    )

    object Record : BottomNavItem(
        route = "record",
        title = "记账",
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircleOutline
    )

    object Stats : BottomNavItem(
        route = "stats",
        title = "统计",
        selectedIcon = Icons.Filled.PieChart,
        unselectedIcon = Icons.Outlined.PieChartOutline
    )

    object Me : BottomNavItem(
        route = "me",
        title = "我的",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.PersonOutline
    )

    companion object {
        val items = listOf(Flow, Record, Stats, Me)
    }
}
