package com.example.myemailapp.presentation.ui.contacts

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.myemailapp.domain.model.ContactFormat
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.presentation.ui.common.LoadingView
import com.example.myemailapp.presentation.ui.common.UserAvatar
import com.example.myemailapp.presentation.ui.common.encodeUriToBase64
import com.example.myemailapp.presentation.ui.common.toolbar.CustomToolbar
import org.koin.androidx.compose.koinViewModel

@Composable
fun ContactScreen(
    navController: NavController,
    viewModel: ContactViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            encodeUriToBase64(context, uri)
                .onSuccess { base64 -> viewModel.updatePhoto(base64) }
                .onFailure { _ ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Could not load photo. Please try again.")
                    }
                }
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(state.processState) {
        if (state.processState == ProcessState.Failure) {
            snackbarHostState.showSnackbar("Failed to load contact")
        }
    }

    LaunchedEffect(state.saveError) {
        if (state.saveError) {
            snackbarHostState.showSnackbar("Failed to save contact. Please try again.")
            viewModel.clearSaveError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .clickable { keyboardController?.hide() },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) { Snackbar(snackbarData = it, containerColor = Color.Red) } },
            topBar = {
                CustomToolbar(
                    title = "Edit contact",
                    onNavigationIconPressed = { navController.popBackStack() },
                    actions = {
                        if (state.processState == ProcessState.Success) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { navController.popBackStack() }) {
                                    Text("Cancel")
                                }
                                TextButton(
                                    onClick = { viewModel.updateContact() },
                                    enabled = state.name.isNotBlank() && state.email.isNotBlank() && !state.isSaving
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (state.processState == ProcessState.Success) {
                EditingContactForm(
                    name = state.name,
                    displayName = state.displayName,
                    email = state.email,
                    format = state.format,
                    photoBase64 = state.photoBase64,
                    onNameChange = viewModel::updateName,
                    onDisplayNameChange = viewModel::updateDisplayName,
                    onEmailChange = viewModel::updateEmail,
                    onFormatChange = viewModel::updateFormat,
                    onPhotoPickerClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.padding(padding)
                )
            }
        }

        if (state.processState == ProcessState.Loading || state.isSaving) {
            LoadingView()
        }
    }
}

@Composable
fun EditingContactForm(
    name: String,
    displayName: String,
    email: String,
    format: ContactFormat,
    photoBase64: String,
    onNameChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onFormatChange: (ContactFormat) -> Unit,
    onPhotoPickerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(100.dp)) {
            UserAvatar(name = name, size = 100.dp, photoBase64 = photoBase64)
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Change photo",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onPhotoPickerClick)
                    .padding(4.dp)
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            label = { Text("Display name (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Email format",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFormatChange(ContactFormat.PLAIN) }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = format == ContactFormat.PLAIN,
                        onClick = { onFormatChange(ContactFormat.PLAIN) }
                    )
                    Text(
                        text = "Plain",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFormatChange(ContactFormat.HTML) }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = format == ContactFormat.HTML,
                        onClick = { onFormatChange(ContactFormat.HTML) }
                    )
                    Text(
                        text = "HTML",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
