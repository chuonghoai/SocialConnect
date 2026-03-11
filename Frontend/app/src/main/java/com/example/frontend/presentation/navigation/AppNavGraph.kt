package com.example.frontend.presentation.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.frontend.presentation.navigation.bottomnav.BottomBar
import com.example.frontend.presentation.navigation.bottomnav.bottomNavItems
import com.example.frontend.presentation.screen.chat.ChatScreen
import com.example.frontend.presentation.screen.conversation.ConversationScreen
import com.example.frontend.presentation.screen.forgotpassword.ForgotPasswordEmailScreen
import com.example.frontend.presentation.screen.forgotpassword.ForgotPasswordOtpScreen
import com.example.frontend.presentation.screen.forgotpassword.ForgotPasswordResetScreen
import com.example.frontend.presentation.screen.home.HomeScreen
import com.example.frontend.presentation.screen.login.LoginScreen
import com.example.frontend.presentation.screen.notification.NotificationScreen
import com.example.frontend.presentation.screen.profile.ProfileScreen
import com.example.frontend.presentation.screen.register.OtpVerificationScreen
import com.example.frontend.presentation.screen.register.RegisterScreen
import com.example.frontend.presentation.screen.register.RegisterViewModel
import com.example.frontend.presentation.screen.search.SearchScreen
import com.example.frontend.presentation.screen.setting.SettingScreen
import com.example.frontend.presentation.screen.video.VideoScreen
import com.example.frontend.presentation.viewmodel.MainViewModel
import com.example.frontend.presentation.viewmodel.SessionViewModel
import com.example.frontend.ui.component.AppNotification
import androidx.navigation.compose.navigation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    sessionViewModel: SessionViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    val currentUser by sessionViewModel.currentUser.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentBaseRoute = currentRoute?.substringBefore("/")

    val bottomBarRoutes = setOf(
        Routes.HOME,
        Routes.SEARCH,
        Routes.VIDEO,
        Routes.NOTIFICATION,
        Routes.PROFILE
    )
    val showBottomBar = currentBaseRoute in bottomBarRoutes

    val notifState by mainViewModel.notificationManager.notification.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    BottomBar(
                        navController = navController,
                        items = bottomNavItems(currentUser?.avatarUrl)
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.SPLASH) {
                    val vm: StartViewModel = hiltViewModel()
                    val dest by vm.startDestination.collectAsState()

                    SplashScreen()

                    LaunchedEffect(dest) {
                        if (dest != null) {
                            if (dest == Routes.HOME) {
                                sessionViewModel.fetchCurrentUser()
                            }

                            navController.navigate(dest!!) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    }
                }

                composable(Routes.LOGIN) {
                    LoginScreen(
                        onLoggedIn = {
                            sessionViewModel.fetchCurrentUser()
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                        onRegisterClick = { navController.navigate(Routes.REGISTER) },
                        onForgotPasswordClick = { navController.navigate(Routes.FORGOT_PASSWORD_EMAIL) }
                    )
                }

                navigation(startDestination = Routes.REGISTER, route = "register_flow") {
                    composable(Routes.REGISTER) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) {
                            navController.getBackStackEntry("register_flow")
                        }
                        val sharedViewModel: RegisterViewModel = hiltViewModel(parentEntry)

                        RegisterScreen(
                            viewModel = sharedViewModel,
                            onLoginClick = { navController.popBackStack() },
                            onSendOtpClick = {
                                navController.navigate(Routes.OTP_SENDED)
                            }
                        )
                    }

                    composable(Routes.OTP_SENDED) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) {
                            navController.getBackStackEntry("register_flow")
                        }
                        val sharedViewModel: RegisterViewModel = hiltViewModel(parentEntry)

                        OtpVerificationScreen(
                            viewModel = sharedViewModel,
                            onBackClick = { navController.popBackStack() },
                            onRegisterClick = {
                                sessionViewModel.fetchCurrentUser()
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo("register_flow") { inclusive = true }
                                }
                            }
                        )
                    }
                }

                composable(Routes.FORGOT_PASSWORD_EMAIL) {
                    ForgotPasswordEmailScreen(
                        onBackClick = { navController.popBackStack() },
                        onOtpSent = { email ->
                            val encodedEmail = Uri.encode(email)
                            navController.navigate("${Routes.FORGOT_PASSWORD_OTP_BASE}?email=$encodedEmail")
                        }
                    )
                }

                composable(Routes.FORGOT_PASSWORD_OTP) { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email")?.let(Uri::decode).orEmpty()

                    ForgotPasswordOtpScreen(
                        onBackClick = { navController.popBackStack() },
                        onOtpVerified = { _, otp ->
                            val encodedEmail = Uri.encode(email)
                            val encodedOtp = Uri.encode(otp)
                            navController.navigate("${Routes.FORGOT_PASSWORD_RESET_BASE}?email=$encodedEmail&otp=$encodedOtp")
                        }
                    )
                }

                composable(Routes.FORGOT_PASSWORD_RESET) {
                    ForgotPasswordResetScreen(
                        onBackClick = { navController.popBackStack() },
                        onResetDone = {
                            sessionViewModel.clearSession()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.PROFILE) {
                    ProfileScreen(
                        onLoggedOut = {
                            sessionViewModel.clearSession()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.PROFILE) { inclusive = true }
                            }
                        },
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onNavigateToSetting = {
                            navController.navigate(Routes.SETTING)
                        }
                    )
                }

                composable(Routes.HOME) {
                    HomeScreen(
                        currentUser = currentUser,
                        onNavigateToMessages = {
                            navController.navigate(Routes.CONVERSATION_LIST)
                        }
                    )
                }

                composable(Routes.SEARCH) {
                    SearchScreen()
                }

                composable(Routes.VIDEO) {
                    VideoScreen()
                }

                composable(Routes.NOTIFICATION) {
                    NotificationScreen()
                }

                composable(Routes.SETTING) {
                    SettingScreen(
                        onBackClick = { navController.popBackStack() },
                        onLoggedOut = {
                            sessionViewModel.clearSession()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.CONVERSATION_LIST) {
                    ConversationScreen(
                        onBackClick = { navController.popBackStack() },
                        onNavigateToChat = { conversationId, conversationName, conversationAvatar ->
                            val encodedName = Uri.encode(conversationName)
                            val encodedAvatar = Uri.encode(conversationAvatar)
                            navController.navigate("${Routes.CHAT_BASE}/$conversationId?name=$encodedName&avatar=$encodedAvatar")
                        }
                    )
                }

                composable(Routes.CHAT) { backStackEntry ->
                    val conversationId =
                        backStackEntry.arguments?.getString("conversationId") ?: "0"
                    val conversationName =
                        backStackEntry.arguments?.getString("name")?.let(Uri::decode)
                            ?: "Người dùng $conversationId"
                    val conversationAvatar =
                        backStackEntry.arguments?.getString("avatar")?.let(Uri::decode)

                    ChatScreen(
                        conversationId = conversationId,
                        conversationName = conversationName,
                        conversationAvatarUrl = conversationAvatar,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }

        AppNotification(
            message = notifState.message,
            type = notifState.type,
            isVisible = notifState.isVisible,
            onDismiss = { mainViewModel.notificationManager.clear() },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
