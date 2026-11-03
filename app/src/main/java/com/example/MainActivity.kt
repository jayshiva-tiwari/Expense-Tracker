package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository
import com.example.ui.AppTab
import com.example.ui.ExpenseViewModel
import com.example.ui.ExpenseViewModelFactory
import com.example.ui.components.ExpenseBottomBar
import com.example.ui.screens.AddExpenseScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SummaryScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ExpenseRepository(database.expenseDao(), applicationContext)
        ExpenseViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                ExpenseTrackerApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ExpenseTrackerApp(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle back button when not on dashboard
    BackHandler(enabled = currentTab != AppTab.DASHBOARD) {
        viewModel.navigateTo(AppTab.DASHBOARD)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ExpenseBottomBar(
                currentTab = currentTab,
                onTabSelected = { tab -> viewModel.navigateTo(tab) }
            )
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentTab,
            label = "tab_crossfade",
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                AppTab.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToAdd = { viewModel.navigateTo(AppTab.ADD_EXPENSE) },
                        onNavigateToSummary = { viewModel.navigateTo(AppTab.SUMMARY) }
                    )
                }

                AppTab.ADD_EXPENSE -> {
                    AddExpenseScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Expense saved successfully!")
                            }
                            viewModel.navigateTo(AppTab.DASHBOARD)
                        }
                    )
                }

                AppTab.SUMMARY -> {
                    SummaryScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
