package com.example.myemailapp.presentation.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myemailapp.domain.model.Condition
import com.example.myemailapp.domain.model.Folder
import com.example.myemailapp.domain.model.Operation
import com.example.myemailapp.domain.model.ProcessState
import com.example.myemailapp.domain.model.Rule
import com.example.myemailapp.presentation.extension.NavControllerExtensions.replaceCurrentRoute
import com.example.myemailapp.presentation.model.Screen
import com.example.myemailapp.presentation.ui.common.UserAvatar
import com.example.myemailapp.presentation.ui.common.drawer.CustomNavigationDrawer
import com.example.myemailapp.presentation.ui.common.drawer.emailsScreenDrawerItems
import com.example.myemailapp.presentation.ui.common.toolbar.CustomToolbar
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

    LaunchedEffect(key1 = state.errorMessage) {
        state.errorMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
            viewModel.clearErrorMessage()
        }
    }

    if (state.showAddRuleDialog) {
        AddRuleDialog(
            folders = state.folders,
            onDismiss = { viewModel.hideAddRuleDialog() },
            onSave = { condition, conditionValue, operation, destinationFolder ->
                viewModel.createRule(condition, conditionValue, operation, destinationFolder)
            }
        )
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Top,
                ) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        val email = state.user?.email ?: ""
                        val displayName = state.user?.displayName?.ifEmpty {
                            email.substringBefore("@").replaceFirstChar { it.uppercase() }
                        } ?: ""
                        ProfileSection(
                            displayName = displayName,
                            email = email
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        RulesHeader(onAddClick = { viewModel.showAddRuleDialog() })
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (state.rules.isEmpty()) {
                        item {
                            Text(
                                text = "No email rules configured",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(state.rules, key = { it.id }) { rule ->
                            RuleItem(
                                rule = rule,
                                onDelete = { viewModel.deleteRule(rule.id) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    )
}

@Composable
private fun ProfileSection(
    displayName: String,
    email: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        UserAvatar(name = displayName, size = 80.dp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = displayName.ifEmpty { "User" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RulesHeader(onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Email Rules",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        IconButton(onClick = onAddClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add rule"
            )
        }
    }
}

@Composable
private fun RuleItem(
    rule: Rule,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${rule.condition.name} ${rule.conditionValue}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildRuleDescription(rule),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete rule",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRuleDialog(
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onSave: (Condition, String, Operation, Folder?) -> Unit
) {
    var selectedCondition by remember { mutableStateOf(Condition.FROM) }
    var conditionValue by remember { mutableStateOf("") }
    var selectedOperation by remember { mutableStateOf(Operation.MOVE) }
    var selectedFolder by remember { mutableStateOf<Folder?>(null) }

    var conditionExpanded by remember { mutableStateOf(false) }
    var operationExpanded by remember { mutableStateOf(false) }
    var folderExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Add Email Rule",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                ExposedDropdownMenuBox(
                    expanded = conditionExpanded,
                    onExpandedChange = { conditionExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCondition.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Condition") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = conditionExpanded,
                        onDismissRequest = { conditionExpanded = false }
                    ) {
                        Condition.entries.forEach { condition ->
                            DropdownMenuItem(
                                text = { Text(condition.name) },
                                onClick = {
                                    selectedCondition = condition
                                    conditionExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = conditionValue,
                    onValueChange = { conditionValue = it },
                    label = { Text("Value") },
                    placeholder = { Text(getConditionPlaceholder(selectedCondition)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = operationExpanded,
                    onExpandedChange = { operationExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedOperation.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Operation") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = operationExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = operationExpanded,
                        onDismissRequest = { operationExpanded = false }
                    ) {
                        Operation.entries.forEach { operation ->
                            DropdownMenuItem(
                                text = { Text(operation.name) },
                                onClick = {
                                    selectedOperation = operation
                                    operationExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedOperation != Operation.DELETE) {
                    Spacer(modifier = Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = folderExpanded,
                        onExpandedChange = { folderExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedFolder?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Destination Folder") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = folderExpanded,
                            onDismissRequest = { folderExpanded = false }
                        ) {
                            if (folders.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No folders available") },
                                    onClick = { folderExpanded = false },
                                    enabled = false
                                )
                            } else {
                                folders.forEach { folder ->
                                    DropdownMenuItem(
                                        text = { Text(folder.name) },
                                        onClick = {
                                            selectedFolder = folder
                                            folderExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(selectedCondition, conditionValue, selectedOperation, selectedFolder)
                        },
                        enabled = conditionValue.isNotBlank() &&
                                (selectedOperation == Operation.DELETE || selectedFolder != null)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

private fun buildRuleDescription(rule: Rule): String {
    return when (rule.operation) {
        Operation.DELETE -> "Delete message"
        Operation.MOVE -> "Move to ${rule.destinationFolderName}"
        Operation.COPY -> "Copy to ${rule.destinationFolderName}"
    }
}

private fun getConditionPlaceholder(condition: Condition): String {
    return when (condition) {
        Condition.TO -> "recipient@example.com"
        Condition.FROM -> "sender@example.com"
        Condition.CC -> "cc@example.com"
        Condition.SUBJECT -> "Subject text to match"
    }
}

@Preview(showBackground = true)
@Composable
fun ProfilePreview() {
    MyEmailAppTheme {
        ProfileScreen(rememberNavController())
    }
}
