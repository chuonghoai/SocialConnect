package com.example.frontend.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.frontend.presentation.navigation.bottomnav.BottomBar
import com.example.frontend.presentation.navigation.bottomnav.bottomNavItems
import com.example.frontend.presentation.screen.home.HomeScreen
import com.example.frontend.presentation.screen.login.LoginScreen
import com.example.frontend.presentation.screen.profile.ProfileScreen
import com.example.frontend.presentation.screen.register.OtpVerificationScreen
import com.example.frontend.presentation.screen.register.RegisterScreen
import com.example.frontend.presentation.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    val currentUser by sessionViewModel.currentUser.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentBaseRoute = currentRoute?.substringBefore("/")

    val bottomBarRoutes = setOf(
        Routes.HOME,
        Routes.PROFILE
    )
    val showBottomBar = currentBaseRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    navController = navController,
                    items = bottomNavItems(currentUser?.avatarUrl)
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(paddingValues)
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

                )
            }

            composable(Routes.REGISTER) {
                RegisterScreen(
                    onLoginClick = { navController.popBackStack() },
                    onSendOtpClick = {
                        navController.navigate(Routes.OTP_SENDED) {
                            popUpTo(Routes.REGISTER) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.OTP_SENDED) {
                OtpVerificationScreen(
                    onBackClick = { navController.popBackStack() },
                    onRegisterClick = {
                        sessionViewModel.fetchCurrentUser()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.OTP_SENDED) { inclusive = true }
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
                    onProfileUpdate = {
                        sessionViewModel.fetchCurrentUser()
                    },
                    navController = navController,
                    userAvatarUrl = currentUser?.avatarUrl
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    currentUser = currentUser
                )
            }
        }
    }
}
