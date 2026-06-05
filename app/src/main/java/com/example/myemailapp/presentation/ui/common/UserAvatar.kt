package com.example.myemailapp.presentation.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image

@Composable
fun UserAvatar(
    name: String,
    size: Dp = 40.dp,
    photoBase64: String = "",
    modifier: Modifier = Modifier
) {
    val bitmap = remember(photoBase64) {
        if (photoBase64.isNotBlank()) decodeBase64ToBitmap(photoBase64, 200, 200) else null
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val initials = buildInitials(name)
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = MaterialTheme.colorScheme.onPrimary,
                style = if (size >= 60.dp) MaterialTheme.typography.headlineMedium
                        else MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun buildInitials(name: String): String {
    if (name.isBlank()) return "?"
    val parts = name.trim().split(" ")
    return when {
        parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
        else -> parts[0].take(2).uppercase()
    }
}
