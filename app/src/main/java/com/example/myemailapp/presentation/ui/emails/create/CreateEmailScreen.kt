package com.example.myemailapp.presentation.ui.emails.create

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myemailapp.domain.model.Attachment
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.presentation.ui.emails.AttachmentViewModel
import com.example.myemailapp.presentation.ui.common.toolbar.CreateEmailScreenToolbarActions
import com.example.myemailapp.presentation.ui.common.toolbar.CustomToolbar
import com.example.myemailapp.ui.theme.MyEmailAppTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEmailScreen(
    navController: NavController,
    replyToEmailId: String? = null,
    replyAllToEmailId: String? = null,
    forwardEmailId: String? = null,
    viewModel: CreateEmailViewModel = koinViewModel(),
    attachmentViewModel: AttachmentViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val attachmentState = attachmentViewModel.state.collectAsStateWithLifecycle().value
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult

        val contentResolver = context.contentResolver

        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@rememberLauncherForActivityResult

        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

        val name = run {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex >= 0) it.getString(nameIndex) else "attachment"
            } ?: "attachment"
        }

        val type = contentResolver.getType(uri) ?: "application/octet-stream"

        val attachment = Attachment(
            id = UUID.randomUUID().toString(),
            dataBase64 = base64,
            type = type,
            name = name
        )

        viewModel.addAttachment(attachment)
    }

    LaunchedEffect(replyToEmailId, replyAllToEmailId, forwardEmailId) {
        replyToEmailId?.let { viewModel.initAsReply(it) }
        replyAllToEmailId?.let { viewModel.initAsReplyAll(it) }
        forwardEmailId?.let { viewModel.initAsForward(it) }
    }

    LaunchedEffect(key1 = state.processState) {
        if (state.processState == ProcessState.Failure) {
            viewModel.resetErrorState()
            scope.launch {
                snackbarHostState.showSnackbar("Error occurred. Try again later.")
            }

            return@LaunchedEffect
        }

        if (state.processState == ProcessState.Success) {
            viewModel.resetEmailResult()
            navController.popBackStack()
            return@LaunchedEffect
        }
    }

    LaunchedEffect(key1 = state.errorMessage) {
        state.errorMessage?.let { errorMessage ->
            scope.launch {
                snackbarHostState.showSnackbar(errorMessage)
                viewModel.clearErrorMessage()
            }
        }
    }

    BackHandler {
        viewModel.saveDraft()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp)
            .clickable { keyboardController?.hide() },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = {
                    Snackbar(
                        snackbarData = it,
                        containerColor = MaterialTheme.colorScheme.error
                    )
                }
            )
        },
        topBar = {
            CustomToolbar(
                title = "Compose",
                actions = {
                    CreateEmailScreenToolbarActions(
                        onSendEmailPressed = {
                            // TODO: Send email and put loading
                            viewModel.sendEmail()
                        },
                        onCancelPressed = navController::popBackStack
                    )
                },
                onNavigationIconPressed = {
                    // TODO: save message to draft
                    viewModel.saveDraft()
//                    navController.popBackStack()
                }
            )
        }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // To field
            OutlinedTextField(
                value = state.to,
                onValueChange = viewModel::updateTo,
                label = { Text("To") },
                placeholder = { Text("Recipient email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Cc/Bcc toggle button (only show when collapsed)
            if (!state.showCcBcc) {
                TextButton(onClick = { viewModel.toggleCcBcc() }) {
                    Text("Add Cc/Bcc")
                }
            }

            // Cc/Bcc fields (only show when expanded)
            AnimatedVisibility(visible = state.showCcBcc) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Cc field
                    OutlinedTextField(
                        value = state.cc,
                        onValueChange = viewModel::updateCc,
                        label = { Text("Cc") },
                        placeholder = { Text("Carbon copy") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Bcc field
                    OutlinedTextField(
                        value = state.bcc,
                        onValueChange = viewModel::updateBcc,
                        label = { Text("Bcc") },
                        placeholder = { Text("Blind carbon copy") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Hide button
                    TextButton(onClick = { viewModel.toggleCcBcc() }) {
                        Text("Hide Cc/Bcc")
                    }
                }
            }

            // Subject field
            OutlinedTextField(
                value = state.subject,
                onValueChange = viewModel::updateSubject,
                label = { Text("Subject") },
                placeholder = { Text("Email subject") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Content field
            OutlinedTextField(
                value = state.content,
                onValueChange = viewModel::updateContent,
                label = { Text("Message") },
                placeholder = { Text("Compose email") },
                singleLine = false,
                minLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            // Add attachment button
            Button(onClick = { launcher.launch("*/*") }) {
                Icon(Icons.Default.Add, contentDescription = "Attach")
                Spacer(Modifier.width(5.dp))
                Text("Add attachment")
            }

            // Attachment list with previews
            if (state.attachments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.attachments.forEach { displayData ->
                        AttachmentPreviewItem(
                            attachmentDisplayData = displayData,
                            onClick = { attachmentViewModel.selectAttachment(displayData) },
                            onRemove = { viewModel.removeAttachment(displayData.attachment.id) }
                        )
                    }
                }
            }

            // Full-screen attachment viewer
            attachmentState.selectedAttachment?.let { displayData ->
                AttachmentViewerDialog(
                    displayData = displayData,
                    onDismiss = { attachmentViewModel.clearSelectedAttachment() }
                )
            }
        }
        }

        // Loading overlay - blocks all touches when visible
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyEmailAppTheme {
        CreateEmailScreen(rememberNavController())
    }
}