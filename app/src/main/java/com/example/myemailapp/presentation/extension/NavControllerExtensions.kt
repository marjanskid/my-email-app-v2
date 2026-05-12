package com.example.myemailapp.presentation.extension

import androidx.navigation.NavController

object NavControllerExtensions  {
    fun NavController.replaceCurrentRoute(route: String) {
        navigate(route) {
            popUpTo(currentBackStackEntry?.destination?.route ?: return@navigate) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }
}