package com.example.myemailapp.presentation.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.myemailapp.ui.theme.MyEmailAppTheme

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1f)
            .clickable(
                enabled = false,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { },
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = 0.7f))
                .blur(16.dp)
        )
        CircularProgressIndicator(strokeWidth = 4.dp)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyEmailAppTheme {
        LoadingView()
    }
}