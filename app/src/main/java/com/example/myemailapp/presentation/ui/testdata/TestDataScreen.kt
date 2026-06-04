package com.example.myemailapp.presentation.ui.testdata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.myemailapp.presentation.extension.NavControllerExtensions.replaceCurrentRoute
import com.example.myemailapp.presentation.model.Screen
import com.example.myemailapp.presentation.ui.common.drawer.CustomNavigationDrawer
import com.example.myemailapp.presentation.ui.common.drawer.emailsScreenDrawerItems
import com.example.myemailapp.presentation.ui.common.toolbar.CustomToolbar
import com.example.myemailapp.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun TestDataScreen(
    navController: NavController,
    viewModel: TestDataViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Handle seeding state
    LaunchedEffect(key1 = state.seedingState) {
        when (val seedingState = state.seedingState) {
            is SeedingState.Success -> {
                snackbarHostState.showSnackbar("Test data seeded successfully!")
                viewModel.resetSeedingState()
            }
            is SeedingState.Error -> {
                snackbarHostState.showSnackbar("Seeding failed: ${seedingState.message}")
                viewModel.resetSeedingState()
            }
            else -> {}
        }
    }

    // Handle clearing state
    LaunchedEffect(key1 = state.clearingState) {
        when (val clearingState = state.clearingState) {
            is ClearingState.Success -> {
                snackbarHostState.showSnackbar("All data cleared successfully!")
                viewModel.resetClearingState()
            }
            is ClearingState.Error -> {
                snackbarHostState.showSnackbar("Clearing failed: ${clearingState.message}")
                viewModel.resetClearingState()
            }
            else -> {}
        }
    }

    val isAnyOperationInProgress = state.seedingState is SeedingState.Seeding ||
            state.clearingState is ClearingState.Clearing

    CustomNavigationDrawer(
        drawerState = drawerState,
        items = emailsScreenDrawerItems,
        onNavDrawerItemPressed = { item ->
            navController.replaceCurrentRoute(item.screen.route)
        },
        selectedItemScreenName = Screen.TestData.route,
        child = {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        snackbar = { snackbarData ->
                            val backgroundColor = when {
                                snackbarData.visuals.message.contains("successfully") -> SuccessGreen
                                else -> Color.Red
                            }
                            Snackbar(
                                snackbarData = snackbarData,
                                containerColor = backgroundColor
                            )
                        }
                    )
                },
                topBar = {
                    CustomToolbar(
                        title = "Test Data",
                        navigationItem = {
                            IconButton(onClick = {
                                scope.launch {
                                    drawerState.apply {
                                        if (isClosed) open() else close()
                                    }
                                }
                            }) {
                                Icon(
                                    Icons.Filled.Menu,
                                    contentDescription = "Menu"
                                )
                            }
                        },
                        onNavigationIconPressed = navController::popBackStack
                    )
                }
            ) { innerPadding ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                ) {
                    Text(
                        text = "Test Data Management",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Button 1: Seed Test Data (Green)
                    Button(
                        onClick = viewModel::seedTestData,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAnyOperationInProgress,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50) // Green
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.seedingState is SeedingState.Seeding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                if (state.seedingState is SeedingState.Seeding)
                                    "Seeding..."
                                else
                                    "Seed Test Data"
                            )
                        }
                    }

                    Text(
                        text = "Creates folders, rules, and emails for all test users",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Button 2: Clear All Data (Red)
                    Button(
                        onClick = viewModel::clearAllData,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAnyOperationInProgress,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336) // Red
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.clearingState is ClearingState.Clearing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                if (state.clearingState is ClearingState.Clearing)
                                    "Clearing..."
                                else
                                    "Clear All Data"
                            )
                        }
                    }

                    Text(
                        text = "Deletes everything from Firestore",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
                    )

                    // Info section
                    Text(
                        text = "Test Users",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "• Dusan Marjanski\n• Marko Markovic\n• Petar Petrovic",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Test Emails: test-001 to test-004",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    )
}
