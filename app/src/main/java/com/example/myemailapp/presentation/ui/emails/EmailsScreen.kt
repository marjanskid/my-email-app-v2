package com.example.myemailapp.presentation.ui.emails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myemailapp.presentation.extension.NavControllerExtensions.replaceCurrentRoute
import com.example.myemailapp.domain.model.EmailResult
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.domain.model.db.Email
import com.example.myemailapp.presentation.model.Screen
import com.example.myemailapp.presentation.ui.common.drawer.CustomNavigationDrawer
import com.example.myemailapp.presentation.ui.common.toolbar.CustomToolbar
import com.example.myemailapp.presentation.ui.common.drawer.emailsScreenDrawerItems
import com.example.myemailapp.presentation.ui.common.toolbar.EmailsScreenToolbarActions
import com.example.myemailapp.ui.theme.MyEmailAppTheme
import com.example.myemailapp.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailsScreen(
    navController: NavController,
    viewModel: EmailsViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(key1 = state.processState) {
        if (state.processState == ProcessState.Failure) {
            scope.launch {
                snackbarHostState.showSnackbar(state.errorMessage ?: "Error occurred. Try again later.")
            }
        }
    }

    // Observe email status events from the service
    LaunchedEffect(key1 = Unit) {
        viewModel.emailStatusEvent.collect { result ->
            val message = when (result) {
                EmailResult.Sent -> "Email sent successfully"
                EmailResult.DraftSaved -> "Draft saved"
                EmailResult.None -> null
            }

            message?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearEmailStatusEvent()
                // Refresh emails after sending
                viewModel.refreshEmails()
            }
        }
    }

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

    CustomNavigationDrawer(
        drawerState = drawerState,
        items = emailsScreenDrawerItems,
        onNavDrawerItemPressed = { item ->
             navController.replaceCurrentRoute(item.screen.route)
        },
        selectedItemScreenName = Screen.Emails.route,
        child = {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { keyboardController?.hide() },
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        snackbar = { snackbarData ->
                            // Green for success, red for errors
                            val backgroundColor = when {
                                snackbarData.visuals.message.startsWith("Email sent") ||
                                snackbarData.visuals.message.startsWith("Draft saved") -> SuccessGreen
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
                        title = "Emails",
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
                        actions = {
                            EmailsScreenToolbarActions(
                                onCreateEmailPressed = {
                                    navController.navigate(Screen.CreateEmail.createRoute())
                                },
                                onFilterPressed = viewModel::updateSearchBarVisibility
                            )
                        },
                        onNavigationIconPressed = navController::popBackStack
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    CustomSearchBar(
                        searchQuery = state.searchQuery,
                        onSearch = viewModel::updateSearchQuery,
                        visible = state.searchBarVisible,
                        onDone = viewModel::clearSearch
                    )
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { viewModel.refreshEmails() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val displayEmails = viewModel.getFilteredEmails()

                        when {
                            state.processState == ProcessState.Loading && state.emails.isEmpty() && !state.isRefreshing -> {
                                // Initial loading state - only show on first load, not during pull-to-refresh
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            state.emails.isEmpty() -> {
                                // Empty state - no emails at all
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No emails yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            displayEmails.isEmpty() && state.searchQuery.isNotEmpty() -> {
                                // No search results
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No emails match \"${state.searchQuery}\"",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {
                                // Email list with lazy loading
                                val listState = rememberLazyListState()

                                // Preload threshold: load more when 3 items from the end
                                val preloadThreshold = 3
                                val shouldLoadMore = remember {
                                    derivedStateOf {
                                        val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                        val totalItems = listState.layoutInfo.totalItemsCount
                                        // Trigger when we're within threshold of the end
                                        lastVisibleItem >= totalItems - preloadThreshold - 1 &&
                                            totalItems > 0 &&
                                            state.hasMore &&
                                            !state.isLoadingMore
                                    }
                                }

                                // Load more emails when scrolling near the end
                                LaunchedEffect(shouldLoadMore.value) {
                                    if (shouldLoadMore.value) {
                                        viewModel.loadMoreEmails()
                                    }
                                }

                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(
                                        items = displayEmails,
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

                                    // Loading indicator at the bottom when loading more
                                    if (state.isLoadingMore) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyEmailAppTheme {
        EmailsScreen(rememberNavController())
    }
}