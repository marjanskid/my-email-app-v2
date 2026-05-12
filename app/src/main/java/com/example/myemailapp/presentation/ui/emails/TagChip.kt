package com.example.myemailapp.presentation.ui.emails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myemailapp.domain.model.Tag
import androidx.core.graphics.toColorInt

@Composable
fun TagChip(
    tag: Tag,
    modifier: Modifier = Modifier
) {
    val backgroundColor = try {
        Color(tag.color.toColorInt())
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primaryContainer
    }

    // Calculate contrasting text color
    val textColor = if (isColorDark(backgroundColor)) Color.White else Color.Black

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = tag.name,
            color = textColor,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun isColorDark(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance < 0.5
}
