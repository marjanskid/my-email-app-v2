package com.example.myemailapp.presentation.ui.common.toolbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.ReplyAll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun EmailsScreenToolbarActions(onCreateEmailPressed: () -> Unit, onFilterPressed: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        IconButton(onClick = onCreateEmailPressed) {
            Icon(
                Icons.Filled.Add,
                ""
            )
        }
        IconButton(onClick = onFilterPressed) {
            Icon(
                Icons.Filled.Search,
                ""
            )
        }
    }
}

@Composable
fun CreateEmailScreenToolbarActions(onSendEmailPressed: () -> Unit, onCancelPressed: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        TextButton(onClick = onSendEmailPressed) {
            Text("Send")
        }
        TextButton(onClick = onCancelPressed) {
            Text("Cancel")
        }
    }
}

@Composable
fun ViewEmailToolbarActions(
    onDeletePressed: () -> Unit,
    onReplyPressed: () -> Unit,
    onReplyAllPressed: () -> Unit,
    onForwardPressed: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        IconButton(onClick = onDeletePressed) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
        IconButton(onClick = onReplyPressed) {
            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Reply")
        }
        IconButton(onClick = onReplyAllPressed) {
            Icon(Icons.AutoMirrored.Filled.ReplyAll, contentDescription = "Reply All")
        }
        IconButton(onClick = onForwardPressed) {
            Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = "Forward")
        }
    }
}