package com.example.frontend.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.frontend.presentation.screen.login.LoginScreen
import com.example.frontend.presentation.screen.profile.ProfileScreen
import com.example.frontend.presentation.screen.register.OtpVerificationScreen
import com.example.frontend.presentation.screen.register.RegisterScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            val vm: StartViewModel = hiltViewModel()
            val dest by vm.startDestination.collectAsState()

            SplashScreen()

            LaunchedEffect(dest) {
                if (dest != null) {
                    navController.navigate(dest!!) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            }
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.PROFILE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onRegisterClick = { navController.navigate(Routes.REGISTER) },

            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.PROFILE) { inclusive = true }
                    }
                }
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
                    navController.navigate(Routes.PROFILE) {
                        popUpTo(Routes.OTP_SENDED) { inclusive = true }
                    }
                }
            )
        }

    }
}
