package com.example.myemailapp.presentation.ui.common.drawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import com.example.myemailapp.presentation.model.NavDrawerItem
import com.example.myemailapp.presentation.model.Screen

val emailsScreenDrawerItems = listOf(
    NavDrawerItem(
        title = "Emails",
        selectedIcon = Icons.Filled.Email,
        unselectedIcon = Icons.Outlined.Email,
        screen = Screen.Emails,
    ),
    NavDrawerItem(
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        screen = Screen.Profile,
    ),
    NavDrawerItem(
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        screen = Screen.Settings,
    ),
    NavDrawerItem(
        title = "Folders",
        selectedIcon = Icons.Filled.Email,
        unselectedIcon = Icons.Outlined.Email,
        screen = Screen.Folders,
    ),
    NavDrawerItem(
        title = "Test Data",
        selectedIcon = Icons.Filled.Science,
        unselectedIcon = Icons.Outlined.Science,
        screen = Screen.TestData,
    ),
)