package com.gaojiluyin.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gaojiluyin.ui.document.DocumentScreen
import com.gaojiluyin.ui.history.HistoryScreen
import com.gaojiluyin.ui.recording.RecordingScreen
import com.gaojiluyin.ui.settings.SettingsScreen
import com.gaojiluyin.ui.update.UpdateDialog
import com.gaojiluyin.ui.update.UpdateViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector?) {
    data object Recording : Screen("recording", "录音", Icons.Default.Mic)
    data object History : Screen("history", "历史", Icons.Default.History)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
    data object Document : Screen("document/{id}", "文档", null) {
        fun createRoute(id: Long) = "document/$id"
    }
}

val bottomNavItems = listOf(Screen.Recording, Screen.History, Screen.Settings)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val updateViewModel: UpdateViewModel = hiltViewModel()

    val showBottomBar = currentDestination?.hierarchy?.any { dest ->
        bottomNavItems.any { it.route == dest.route }
    } == true

    var showUpdateDialog by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdate()
    }

    if (showUpdateDialog) {
        UpdateDialog(
            viewModel = updateViewModel,
            onDismiss = { showUpdateDialog = false }
        )
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Recording.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Recording.route) {
                RecordingScreen(
                    onNavigateToDocument = { id ->
                        navController.navigate(Screen.Document.createRoute(id))
                    }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onRecordingClick = { id ->
                        navController.navigate(Screen.Document.createRoute(id))
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = Screen.Document.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val recordingId = backStackEntry.arguments?.getLong("id") ?: return@composable
                DocumentScreen(recordingId = recordingId)
            }
        }
    }
}
