package com.example.myemailapp.presentation.ui.folders.view

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.myemailapp.domain.model.Folder
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.domain.model.db.Email
import com.example.myemailapp.presentation.extension.NavControllerExtensions.replaceCurrentRoute
import com.example.myemailapp.presentation.model.Screen
import com.example.myemailapp.presentation.ui.common.LoadingView
import com.example.myemailapp.presentation.ui.common.drawer.CustomNavigationDrawer
import com.example.myemailapp.presentation.ui.common.drawer.emailsScreenDrawerItems
import com.example.myemailapp.presentation.ui.common.toolbar.CustomToolbar
import com.example.myemailapp.presentation.ui.emails.EmailListItem
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ViewFolderScreen(
    navController: NavController,
    viewModel: ViewFolderViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val keyboardController = LocalSoftwareKeyboardController.current
    val state = viewModel.state.collectAsStateWithLifecycle().value

    // Observe savedStateHandle for optimistic updates from ViewEmailScreen
    LaunchedEffect(Unit) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

        // Handle updated email (read/star/tag changes)
        savedStateHandle?.getStateFlow<Email?>("updated_email", null)?.collect { email ->
            email?.let {
                viewModel.updateEmailLocally(it)
                savedStateHandle.remove<Email>("updated_email")
            }
        }
    }

    LaunchedEffect(Unit) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

        // Handle deleted email
        savedStateHandle?.getStateFlow<String?>("deleted_email_id", null)?.collect { emailId ->
            emailId?.let {
                viewModel.removeEmailLocally(it)
                savedStateHandle.remove<String>("deleted_email_id")
            }
        }
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
                            title = state.folder?.name ?: "Folder",
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
                            onNavigationIconPressed = navController::popBackStack,
                            actions = {
                                IconButton(onClick = {
                                    // Navigate to edit folder screen
                                    state.folder?.let {
                                        navController.navigate(Screen.EditFolder.createRoute(it.id))
                                    }
                                }) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Edit folder"
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Folder info card
                        state.folder?.let { folder ->
                            FolderInfoCard(
                                folder = folder,
                                emailCount = state.emails.size,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        // Emails list
                        when {
                            state.processState == ProcessState.Loading -> {
                                // Loading handled by overlay spinner
                            }
                            state.emails.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No emails in this folder",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(
                                        items = state.emails,
                                        key = { it.id }
                                    ) { email ->
                                        EmailListItem(
                                            email = email,
                                            onClick = {
                                                navController.navigate(Screen.ViewEmail.createRoute(email.id))
                                            },
                                            onStarClick = {
                                                viewModel.toggleStar(email.id, email.isStarred)
                                            }
                                        )
                                    }
                                }
                            }
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
private fun FolderInfoCard(
    folder: Folder,
    emailCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$emailCount ${if (emailCount == 1) "message" else "messages"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Created: ${formatCreatedAt(folder)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatCreatedAt(folder: Folder): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        .withZone(ZoneId.systemDefault())
    return formatter.format(folder.createdAt)
}
