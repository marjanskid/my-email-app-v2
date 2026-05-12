package com.example.myemailapp.presentation.ui.folders.create_new

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.domain.model.isFailure
import com.example.myemailapp.domain.model.isSuccess
import com.example.myemailapp.presentation.ui.common.toolbar.CustomToolbar
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateFolderScreen(
    navController: NavController,
    viewModel: CreateFolderViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val state = viewModel.state.collectAsStateWithLifecycle().value

    LaunchedEffect(key1 = state.processState) {
        if (state.processState.isFailure) {
            scope.launch {
                snackbarHostState.showSnackbar("Error occurred. Try again later.")
            }
            return@LaunchedEffect
        }

        if (state.processState.isSuccess) {
            navController.popBackStack()
            return@LaunchedEffect
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    title = "Create new folder",
                    onNavigationIconPressed = navController::popBackStack,
                    actions = {
                        Row {
                            TextButton(
                                onClick = navController::popBackStack
                            ) {
                                Text("Cancel")
                            }
                            TextButton(
                                onClick = viewModel::createFolder
                            ) {
                                Text("Save")
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::updateFolderName,
                    label = { Text("Name") },
                    placeholder = { Text("New folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (state.processState == ProcessState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}