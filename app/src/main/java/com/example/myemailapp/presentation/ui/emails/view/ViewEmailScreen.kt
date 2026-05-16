package com.example.myemailapp.presentation.ui.emails.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.myemailapp.presentation.model.AttachmentDisplayData
import com.example.myemailapp.domain.model.EmailResult
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.presentation.model.Screen
import com.example.myemailapp.domain.model.db.Email
import com.example.myemailapp.presentation.ui.common.LoadingView
import com.example.myemailapp.presentation.ui.common.toolbar.CustomToolbar
import com.example.myemailapp.presentation.ui.common.toolbar.ViewEmailToolbarActions
import com.example.myemailapp.presentation.ui.emails.create.AttachmentPreviewItem
import com.example.myemailapp.presentation.ui.emails.create.AttachmentViewerDialog
import com.example.myemailapp.presentation.ui.emails.TagChip
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewEmailScreen(
    navController: NavController,
    emailId: String,
    viewModel: ViewEmailViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Activity result launcher for individual file save
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(state.attachmentPendingSave?.type ?: "*/*")
    ) { uri ->
        uri?.let {
            state.attachmentPendingSave?.let { attachment ->
                viewModel.saveAttachmentToUri(it, attachment, context)
            }
        }
    }

    // Activity result launcher for folder selection (save all)
    val saveFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            state.email?.attachments?.let { attachments ->
                viewModel.saveAllAttachmentsToFolder(it, attachments, context)
            }
        }
    }

    // Load email when screen opens
    LaunchedEffect(emailId) {
        viewModel.loadEmail(emailId)
    }

    // Handle email deleted - navigate back and pass the deleted email ID
    LaunchedEffect(state.emailDeleted) {
        if (state.emailDeleted) {
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("deleted_email_id", emailId)
            navController.popBackStack()
        }
    }

    // Launch file picker when attachment pending save
    LaunchedEffect(state.attachmentPendingSave) {
        state.attachmentPendingSave?.let { attachment ->
            saveFileLauncher.launch(attachment.name)
        }
    }

    // Success message for saved attachments
    LaunchedEffect(state.savedAttachmentIds.size) {
        if (state.savedAttachmentIds.size == 1 && !state.savingAllAttachments) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Attachment saved",
                    duration = SnackbarDuration.Short
                )
            }
        } else if (state.savedAttachmentIds.size > 1 && !state.savingAllAttachments && state.savedAttachmentIds.isNotEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "${state.savedAttachmentIds.size} attachments saved",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Handle errors
    LaunchedEffect(state.processState) {
        if (state.processState == ProcessState.Failure) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = state.errorMessage ?: "An error occurred",
                    duration = SnackbarDuration.Short
                )
            }
            viewModel.resetProcessState()
        }
    }

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
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = {
                    Snackbar(
                        snackbarData = it,
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                }
            )
        },
        topBar = {
            CustomToolbar(
                title = "Email",
                onNavigationIconPressed = {
                    // Pass updated email back to EmailsScreen for optimistic update
                    state.email?.let { email ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("updated_email", email)
                    }
                    navController.popBackStack()
                },
                actions = {
                    ViewEmailToolbarActions(
                        onDeletePressed = { viewModel.deleteEmail() },
                        onReplyPressed = {
                            state.email?.id?.let { emailId ->
                                navController.navigate(Screen.CreateEmail.createRoute(replyToEmailId = emailId))
                            }
                        },
                        onReplyAllPressed = {
                            state.email?.id?.let { emailId ->
                                navController.navigate(Screen.CreateEmail.createRoute(replyAllToEmailId = emailId))
                            }
                        },
                        onForwardPressed = {
                            state.email?.id?.let { emailId ->
                                navController.navigate(Screen.CreateEmail.createRoute(forwardEmailId = emailId))
                            }
                        }
                    )
                }
            )
        }
    ) { innerPadding ->
        when {
            state.processState == ProcessState.Loading -> {
                LoadingView()
            }

            state.email != null -> {
                EmailContent(
                    email = state.email,
                    isStarred = state.email.isStarred,
                    onStarToggle = { viewModel.toggleStar() },
                    attachmentDisplayDataList = state.attachmentDisplayDataList,
                    onAttachmentClick = { viewModel.selectAttachment(it) },
                    onAttachmentSave = { viewModel.requestSaveAttachment(it) },
                    savingAttachmentIds = state.savingAttachmentIds,
                    savedAttachmentIds = state.savedAttachmentIds,
                    onSaveAll = { saveFolderLauncher.launch(null) },
                    savingAllAttachments = state.savingAllAttachments,
                    saveAllProgress = state.saveAllProgress,
                    onManageTags = { viewModel.showTagSelection() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }

        // Attachment viewer dialog
        state.selectedAttachment?.let { displayData ->
            AttachmentViewerDialog(
                displayData = displayData,
                onDismiss = { viewModel.clearSelectedAttachment() }
            )
        }

        // Tag selection dialog
        if (state.showTagDialog) {
            TagSelectionDialog(
                currentTags = state.email?.tags ?: emptyList(),
                onTagToggle = { tag -> viewModel.toggleTag(tag) },
                onDismiss = { viewModel.hideTagSelection() }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmailContent(
    email: Email,
    isStarred: Boolean,
    onStarToggle: () -> Unit,
    attachmentDisplayDataList: List<AttachmentDisplayData>,
    onAttachmentClick: (AttachmentDisplayData) -> Unit,
    onAttachmentSave: (AttachmentDisplayData) -> Unit,
    savingAttachmentIds: Set<String>,
    savedAttachmentIds: Set<String>,
    onSaveAll: () -> Unit,
    savingAllAttachments: Boolean,
    saveAllProgress: Int,
    onManageTags: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Email Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = email.subject,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onStarToggle) {
                        Icon(
                            imageVector = if (isStarred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = if (isStarred) "Unstar" else "Star",
                            tint = if (isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = email.dateTime
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                EmailField(label = "From", value = email.from)
                EmailField(label = "To", value = email.to)
                if (email.cc.isNotBlank()) {
                    EmailField(label = "CC", value = email.cc)
                }
                if (email.bcc.isNotBlank()) {
                    EmailField(label = "BCC", value = email.bcc)
                }
            }
        }

        // Email Body Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = email.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Tags Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FilledTonalIconButton(
                        onClick = onManageTags,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Manage tags",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (email.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        email.tags.forEach { tag ->
                            TagChip(tag = tag)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No tags yet. Tap + to add tags.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Attachments Section
        if (attachmentDisplayDataList.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attachments (${attachmentDisplayDataList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (savingAllAttachments) {
                    Text(
                        text = "Saving $saveAllProgress/${attachmentDisplayDataList.size}...",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    OutlinedButton(
                        onClick = onSaveAll,
                        enabled = !savingAllAttachments
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Save All")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                attachmentDisplayDataList.forEach { displayData ->
                    AttachmentPreviewItem(
                        attachmentDisplayData = displayData,
                        onClick = { onAttachmentClick(displayData) },
                        onSave = { onAttachmentSave(displayData) },
                        isSaving = displayData.attachment.id in savingAttachmentIds,
                        isSaved = displayData.attachment.id in savedAttachmentIds
                    )
                }
            }
        }
    }
}

@Composable
private fun EmailField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
