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
import androidx.compose.material3.OutlinedTextField
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

    LaunchedEffect(key1 = state.clearingState) {
        when (val clearingState = state.clearingState) {
            is ClearingState.Success -> {
                snackbarHostState.showSnackbar("Test data cleared successfully!")
                viewModel.resetClearingState()
            }
            is ClearingState.Error -> {
                snackbarHostState.showSnackbar("Clearing failed: ${clearingState.message}")
                viewModel.resetClearingState()
            }
            else -> {}
        }
    }

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

                    // Email ID input section
                    OutlinedTextField(
                        value = state.testEmailId,
                        onValueChange = viewModel::updateTestEmailId,
                        label = { Text("Enter Email ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        text = "Test Email IDs: email-001, email-002, email-003, email-004, email-005",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (state.testEmailId.isNotBlank()) {
                                navController.navigate(Screen.ViewEmail.createRoute(state.testEmailId))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.testEmailId.isNotBlank()
                    ) {
                        Text("View Email")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Seed Test Data button
                    Button(
                        onClick = viewModel::seedTestData,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.seedingState !is SeedingState.Seeding && state.clearingState !is ClearingState.Clearing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Clear Test Data button
                    Button(
                        onClick = viewModel::clearTestData,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.clearingState !is ClearingState.Clearing && state.seedingState !is SeedingState.Seeding,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
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
                                    "Clear Test Data"
                            )
                        }
                    }
                }
            }
        }
    )
}
