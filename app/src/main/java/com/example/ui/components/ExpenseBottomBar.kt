package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.AppTab

@Composable
fun ExpenseBottomBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("bottom_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = MaterialTheme.colorScheme.surfaceVariant.let { androidx.compose.ui.unit.Dp(8f) }
    ) {
        // Tab 1: Dashboard
        val isDashboard = currentTab == AppTab.DASHBOARD
        NavigationBarItem(
            selected = isDashboard,
            onClick = { onTabSelected(AppTab.DASHBOARD) },
            icon = {
                Icon(
                    imageVector = if (isDashboard) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                    contentDescription = "Dashboard"
                )
            },
            label = { Text("Dashboard") },
            modifier = Modifier.testTag("tab_dashboard"),
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )

        // Tab 2: Add Expense
        val isAdd = currentTab == AppTab.ADD_EXPENSE
        NavigationBarItem(
            selected = isAdd,
            onClick = { onTabSelected(AppTab.ADD_EXPENSE) },
            icon = {
                Icon(
                    imageVector = if (isAdd) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline,
                    contentDescription = "Add Expense"
                )
            },
            label = { Text("Add") },
            modifier = Modifier.testTag("tab_add_expense"),
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )

        // Tab 3: Summary / Categories
        val isSummary = currentTab == AppTab.SUMMARY
        NavigationBarItem(
            selected = isSummary,
            onClick = { onTabSelected(AppTab.SUMMARY) },
            icon = {
                Icon(
                    imageVector = if (isSummary) Icons.Filled.PieChart else Icons.Outlined.PieChart,
                    contentDescription = "Categories Summary"
                )
            },
            label = { Text("Categories") },
            modifier = Modifier.testTag("tab_categories"),
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
