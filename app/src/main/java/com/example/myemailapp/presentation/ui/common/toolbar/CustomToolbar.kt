package com.example.myemailapp.presentation.ui.common.toolbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.myemailapp.ui.theme.MyEmailAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomToolbar(
    title: String,
    actions: @Composable () -> Unit = {},
    onNavigationIconPressed: () -> Unit,
    navigationItem: @Composable (onPressed: () -> Unit) -> Unit = { DefaultNavigationItem { onNavigationIconPressed() } },
) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = { navigationItem { onNavigationIconPressed() } },
        actions = { actions() },
    )
}

@Composable
fun DefaultNavigationItem(onBackPressed: () -> Unit) {
    IconButton(onClick = { onBackPressed() }) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            ""
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CustomToolbarPreview() {
    MyEmailAppTheme {
        CustomToolbar(title = "Toolbar title", onNavigationIconPressed = {})
    }
}