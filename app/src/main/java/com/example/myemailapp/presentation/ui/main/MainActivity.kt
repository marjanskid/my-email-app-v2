package com.example.myemailapp.presentation.ui.main

import NoInternetConnectionScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myemailapp.presentation.model.Screen
import com.example.myemailapp.presentation.extension.ContextExtensions.isInternetAvailable
import com.example.myemailapp.presentation.ui.folders.view_all.FoldersScreen
import com.example.myemailapp.presentation.settings.SettingsScreen
import com.example.myemailapp.presentation.ui.emails.EmailsScreen
import com.example.myemailapp.presentation.ui.emails.create.CreateEmailScreen
import com.example.myemailapp.presentation.ui.emails.view.ViewEmailScreen
import com.example.myemailapp.presentation.ui.folders.create_new.CreateFolderScreen
import com.example.myemailapp.presentation.ui.folders.edit.EditFolderScreen
import com.example.myemailapp.presentation.ui.folders.view.ViewFolderScreen
import com.example.myemailapp.presentation.ui.login.LoginScreen
import com.example.myemailapp.presentation.ui.profile.ProfileScreen
import com.example.myemailapp.presentation.ui.testdata.TestDataScreen
import com.example.myemailapp.ui.theme.MyEmailAppTheme
import com.google.firebase.FirebaseApp
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseApp.initializeApp(this)

        val hasInternet = isInternetAvailable()

        installSplashScreen().setKeepOnScreenCondition {
            mainViewModel.state.value.isLoading
        }

        setContent {
            MyEmailAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    val state = mainViewModel.state.collectAsStateWithLifecycle().value

                    NavHost(
                        navController = navController,
                        startDestination = configureStartingDestination(
                            hasInternet = hasInternet,
                            isUserLoggedIn = state.userLoggedIn
                        )
                    ) {
                        composable(route = Screen.Login.route) {
                            LoginScreen(navController = navController)
                        }
                        composable(
                            route = Screen.NoInternetConnection.route,
                        ) {
                            NoInternetConnectionScreen(navController = navController)
                        }
                        composable(
                            route = Screen.Emails.route,
                        ) {
                            EmailsScreen(navController = navController)
                        }
                        composable(
                            route = Screen.Profile.route,
                        ) {
                            ProfileScreen(navController = navController)
                        }
                        composable(
                            route = Screen.Settings.route,
                        ) {
                            SettingsScreen(navController = navController)
                        }
                        composable(
                            route = Screen.Folders.route,
                        ) {
                            FoldersScreen(navController = navController)
                        }
                        composable(
                            route = Screen.CreateEmail.route,
                            arguments = listOf(
                                navArgument("replyToEmailId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                },
                                navArgument("replyAllToEmailId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                },
                                navArgument("forwardEmailId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val replyToEmailId = backStackEntry.arguments?.getString("replyToEmailId")
                            val replyAllToEmailId = backStackEntry.arguments?.getString("replyAllToEmailId")
                            val forwardEmailId = backStackEntry.arguments?.getString("forwardEmailId")
                            CreateEmailScreen(
                                navController = navController,
                                replyToEmailId = replyToEmailId,
                                replyAllToEmailId = replyAllToEmailId,
                                forwardEmailId = forwardEmailId
                            )
                        }
                        composable(
                            route = Screen.CreateFolder.route,
                        ) {
                            CreateFolderScreen(navController = navController)
                        }
                        composable(
                            route = Screen.ViewFolder.route,
                            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
                        ) { _ ->
                            ViewFolderScreen(
                                navController = navController
                            )
                        }
                        composable(
                            route = Screen.EditFolder.route,
                            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
                        ) { _ ->
                            EditFolderScreen(
                                navController = navController
                            )
                        }
                        composable(
                            route = Screen.TestData.route,
                        ) {
                            TestDataScreen(navController = navController)
                        }
                        composable(
                            route = Screen.ViewEmail.route,
                            arguments = listOf(navArgument("emailId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val emailId = backStackEntry.arguments?.getString("emailId") ?: return@composable
                            ViewEmailScreen(
                                navController = navController,
                                emailId = emailId
                            )
                        }
                    }
                }
            }
        }
    }

    private fun configureStartingDestination(
        hasInternet: Boolean,
        isUserLoggedIn: Boolean
    ): String {
        if (!hasInternet) {
            return Screen.NoInternetConnection.route
        }

        return if (isUserLoggedIn) Screen.Emails.route else Screen.Login.route
    }
}