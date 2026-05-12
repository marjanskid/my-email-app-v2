package com.example.myemailapp.presentation.ui.login

import com.example.myemailapp.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.SegmentedButtonDefaults.IconSize
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.presentation.model.Screen
import com.example.myemailapp.domain.model.isLoading
import com.example.myemailapp.presentation.ui.common.LoadingView
import com.example.myemailapp.ui.theme.MyEmailAppTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(navController: NavController, viewModel: LoginViewModel = koinViewModel()) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(key1 = state.userLoggedIn) {
        if (state.userLoggedIn) {
            navController.navigate(Screen.Emails.route) {
                navController.popBackStack()
            }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { keyboardController?.hide() }
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    snackbar = { Snackbar(snackbarData = it, containerColor = Color.Red) }
                )
            },
        ) { innerPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(modifier = Modifier.height(20.dp))
                TextField(
                    label = {
                        Text("Email")
                    },
                    value = state.email,
                    onValueChange = viewModel::updateEmail
                )
                Box(modifier = Modifier.height(20.dp))
                TextField(
                    value = state.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = viewModel::togglePasswordVisibility) {
                            Icon(
                                state.showPassword,
                                activeContent = {
                                    androidx.compose.material3.Icon(
                                        painterResource(R.drawable.ic_visibility),
                                        contentDescription = "Password visible",
                                        modifier = Modifier.size(IconSize)
                                    )
                                },
                                inactiveContent = {
                                    androidx.compose.material3.Icon(
                                        painterResource(R.drawable.ic_visibility_off),
                                        contentDescription = "Password not visible",
                                        modifier = Modifier.size(IconSize)
                                    )
                                }
                            )
                        }
                    }
                )
                Box(modifier = Modifier.height(40.dp))
                Button(
                    onClick = { viewModel.login() },
                    enabled = !state.processState.isLoading
                ) {
                    Text("Login")
                }
            }
        }

        if (state.processState == ProcessState.Loading) {
            LoadingView()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyEmailAppTheme {
        LoginScreen(rememberNavController())
    }
}