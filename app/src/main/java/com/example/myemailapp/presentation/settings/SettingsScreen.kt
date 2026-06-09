package com.example.myemailapp.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.myemailapp.domain.model.settings.RefreshInterval
import com.example.myemailapp.domain.model.settings.SortOrder
import com.example.myemailapp.presentation.extension.NavControllerExtensions.replaceCurrentRoute
import com.example.myemailapp.presentation.model.Screen
import com.example.myemailapp.presentation.ui.common.drawer.CustomNavigationDrawer
import com.example.myemailapp.presentation.ui.common.toolbar.CustomToolbar
import com.example.myemailapp.presentation.ui.common.drawer.emailsScreenDrawerItems
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val keyboardController = LocalSoftwareKeyboardController.current
    val viewModel: SettingsViewModel = koinViewModel()

    val refreshInterval by viewModel.refreshInterval.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val refreshIntervalExpanded by viewModel.refreshIntervalExpanded.collectAsStateWithLifecycle()

    CustomNavigationDrawer(
        drawerState = drawerState,
        items = emailsScreenDrawerItems,
        onNavDrawerItemPressed = { item ->
            navController.replaceCurrentRoute(item.screen.route)
        },
        selectedItemScreenName = Screen.Settings.route,
        child = {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { keyboardController?.hide() },
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        snackbar = { Snackbar(snackbarData = it, containerColor = Color.Red) }
                    )
                },
                topBar = {
                    CustomToolbar(
                        title = "Settings",
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
                                    ""
                                )
                            }
                        },
                        onNavigationIconPressed = navController::popBackStack
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    // Refresh Interval Card
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Email Refresh Interval",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            ExposedDropdownMenuBox(
                                expanded = refreshIntervalExpanded,
                                onExpandedChange = { viewModel.setRefreshIntervalExpanded(it) }
                            ) {
                                OutlinedTextField(
                                    value = refreshInterval.label,
                                    onValueChange = { /* read-only field, no-op required by API */ },
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = refreshIntervalExpanded
                                        )
                                    },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = refreshIntervalExpanded,
                                    onDismissRequest = { viewModel.setRefreshIntervalExpanded(false) }
                                ) {
                                    RefreshInterval.entries.forEach { interval ->
                                        DropdownMenuItem(
                                            text = { Text(interval.label) },
                                            onClick = {
                                                viewModel.setRefreshInterval(interval)
                                                viewModel.setRefreshIntervalExpanded(false)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sort Order Card
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Message Sorting",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Newest First (Descending)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setSortOrder(SortOrder.DESCENDING)
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = sortOrder == SortOrder.DESCENDING,
                                    onClick = { viewModel.setSortOrder(SortOrder.DESCENDING) }
                                )
                                Text(
                                    text = "Newest first",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            // Oldest First (Ascending)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setSortOrder(SortOrder.ASCENDING)
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = sortOrder == SortOrder.ASCENDING,
                                    onClick = { viewModel.setSortOrder(SortOrder.ASCENDING) }
                                )
                                Text(
                                    text = "Oldest first",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}