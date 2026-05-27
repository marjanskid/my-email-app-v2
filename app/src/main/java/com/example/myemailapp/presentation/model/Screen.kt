package com.example.myemailapp.presentation.model

sealed class Screen(val route: String) {
    object Login: Screen("login-screen")
    object NoInternetConnection: Screen("no-internet-connection-screen")
    object Emails: Screen("emails-screen")
    object ViewEmail : Screen("view-email-screen/{emailId}") {
        fun createRoute(emailId: String) = "view-email-screen/$emailId"
    }
    object Profile: Screen("profile-screen")
    object Settings: Screen("settings-screen")
    object Folders: Screen("folders-screen")
    object CreateEmail: Screen("create-email-screen?replyToEmailId={replyToEmailId}&replyAllToEmailId={replyAllToEmailId}&forwardEmailId={forwardEmailId}") {
        fun createRoute(
            replyToEmailId: String? = null,
            replyAllToEmailId: String? = null,
            forwardEmailId: String? = null
        ): String {
            return buildString {
                append("create-email-screen")
                val params = mutableListOf<String>()
                replyToEmailId?.let { params.add("replyToEmailId=$it") }
                replyAllToEmailId?.let { params.add("replyAllToEmailId=$it") }
                forwardEmailId?.let { params.add("forwardEmailId=$it") }
                if (params.isNotEmpty()) {
                    append("?")
                    append(params.joinToString("&"))
                }
            }
        }
    }
    object CreateFolder: Screen("create-folder-screen")
    object ViewFolder : Screen("view-folder-screen/{folderId}") {
        fun createRoute(folderId: String) = "view-folder-screen/$folderId"
    }
    object EditFolder : Screen("edit-folder-screen/{folderId}") {
        fun createRoute(folderId: String) = "edit-folder-screen/$folderId"
    }
    object TestData: Screen("test-data-screen")
}