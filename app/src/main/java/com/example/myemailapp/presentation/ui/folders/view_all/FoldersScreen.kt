package com.example.myemailapp.presentation.ui.folders.view_all

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.myemailapp.presentation.extension.NavControllerExtensions.replaceCurrentRoute
import com.example.myemailapp.domain.model.FolderWithCount
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.presentation.model.Screen
import com.example.myemailapp.presentation.ui.common.LoadingView
import com.example.myemailapp.presentation.ui.common.drawer.CustomNavigationDrawer
import com.example.myemailapp.presentation.ui.common.toolbar.CustomToolbar
import com.example.myemailapp.presentation.ui.common.drawer.emailsScreenDrawerItems
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun FoldersScreen(
    navController: NavController,
    viewModel: FoldersViewModel = koinViewModel(),
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val keyboardController = LocalSoftwareKeyboardController.current
    val state = viewModel.state.collectAsStateWithLifecycle().value

    LaunchedEffect(Unit) {
        viewModel.getFolders()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CustomNavigationDrawer(
            drawerState = drawerState,
            items = emailsScreenDrawerItems,
            onNavDrawerItemPressed = { item ->
                navController.replaceCurrentRoute(item.screen.route)
            },
            selectedItemScreenName = Screen.Folders.route,
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
                            title = "Folders",
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
                            onNavigationIconPressed = navController::popBackStack,
                            actions = {
                                IconButton(onClick = {
                                    navController.navigate(Screen.CreateFolder.route)
                                }) {
                                    Icon(
                                        Icons.Filled.Add,
                                        ""
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.folders) { folderWithCount ->
                            FolderItemCard(
                                folderWithCount = folderWithCount,
                                onClick = {
                                    navController.navigate(Screen.ViewFolder.createRoute(folderWithCount.folder.id))
                                }
                            )
                        }
                    }
                }
            }
        )

        if (state.processState == ProcessState.Loading) {
            LoadingView()
        }
    }
}

@Composable
fun FolderItemCard(folderWithCount: FolderWithCount, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = folderWithCount.folder.name,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Total messages: ${folderWithCount.messageCount}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}