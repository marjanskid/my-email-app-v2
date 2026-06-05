package com.example.myemailapp.presentation.ui.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.myemailapp.domain.model.Contact
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.domain.model.displayLabel
import com.example.myemailapp.presentation.extension.NavControllerExtensions.replaceCurrentRoute
import com.example.myemailapp.presentation.model.Screen
import com.example.myemailapp.presentation.ui.common.LoadingView
import com.example.myemailapp.presentation.ui.common.UserAvatar
import com.example.myemailapp.presentation.ui.common.drawer.CustomNavigationDrawer
import com.example.myemailapp.presentation.ui.common.drawer.emailsScreenDrawerItems
import com.example.myemailapp.presentation.ui.common.toolbar.ContactsScreenToolbarActions
import com.example.myemailapp.presentation.ui.common.toolbar.CustomToolbar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun ContactsScreen(
    navController: NavController,
    viewModel: ContactsViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.processState) {
        if (state.processState == ProcessState.Failure) {
            snackbarHostState.showSnackbar("Failed to load contacts")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CustomNavigationDrawer(
            drawerState = drawerState,
            items = emailsScreenDrawerItems,
            selectedItemScreenName = Screen.Contacts.route,
            onNavDrawerItemPressed = { item ->
                navController.replaceCurrentRoute(item.screen.route)
            },
            child = {
                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        CustomToolbar(
                            title = "Contacts",
                            actions = {
                                ContactsScreenToolbarActions(onNewContact = {
                                    navController.navigate(Screen.CreateContact.route)
                                })
                            },
                            onNavigationIconPressed = {
                                scope.launch { drawerState.open() }
                            },
                            navigationItem = { onPressed ->
                                IconButton(onClick = onPressed) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                }
                            }
                        )
                    }
                ) { padding ->
                    when {
                        state.processState == ProcessState.Loading -> {}

                        state.contacts.isNotEmpty() -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                            ) {
                                items(state.contacts) { contact ->
                                    ContactListItem(
                                        contact = contact,
                                        onClick = {
                                            navController.navigate(Screen.Contact.createRoute(contact.id))
                                        }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }

                        state.processState == ProcessState.Success -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No contacts yet. Tap + to create one.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        state.processState == ProcessState.Failure -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(onClick = { viewModel.loadContacts() }) {
                                    Text("Retry")
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
fun ContactListItem(
    contact: Contact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(name = contact.name, photoBase64 = contact.photoBase64)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = contact.displayLabel(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = contact.email,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
