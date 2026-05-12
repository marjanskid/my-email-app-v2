package com.example.myemailapp.presentation.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myemailapp.presentation.extension.NavControllerExtensions.replaceCurrentRoute
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.presentation.model.Screen
import com.example.myemailapp.presentation.ui.common.drawer.CustomNavigationDrawer
import com.example.myemailapp.presentation.ui.common.toolbar.CustomToolbar
import com.example.myemailapp.presentation.ui.common.drawer.emailsScreenDrawerItems
import com.example.myemailapp.presentation.ui.common.toolbar.EmailsScreenToolbarActions
import com.example.myemailapp.presentation.ui.emails.CustomSearchBar
import com.example.myemailapp.presentation.ui.emails.EmailsViewModel
import com.example.myemailapp.ui.theme.MyEmailAppTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(key1 = state.userLoggedOut) {
        if (!state.userLoggedOut) {
            return@LaunchedEffect
        }

        navController.navigate(Screen.Login.route) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(key1 = state.processState) {
        if (state.processState == ProcessState.Failure) {
            viewModel.resetProcessState()
            scope.launch {
                snackbarHostState.showSnackbar("Error occurred. Try again later.")
            }
        }
    }

    CustomNavigationDrawer(
        drawerState = drawerState,
        items = emailsScreenDrawerItems,
        onNavDrawerItemPressed = { item ->
             navController.replaceCurrentRoute(item.screen.route)
        },
        selectedItemScreenName = Screen.Profile.route,
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
                        title = "Profile",
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
                            TextButton(onClick = { viewModel.logout() }) {
                                Text("Logout")
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
                        .padding(innerPadding),
                ) {

                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ProfilePreview() {
    MyEmailAppTheme {
        ProfileScreen(rememberNavController())
    }
}