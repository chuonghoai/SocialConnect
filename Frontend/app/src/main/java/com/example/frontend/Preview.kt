//package com.example.frontend.presentation.screen
//
//import android.content.res.Configuration
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.tooling.preview.Preview
//import com.example.frontend.core.network.ApiResult
//import com.example.frontend.core.network.TokenProvider
//import com.example.frontend.domain.model.User
//import com.example.frontend.domain.repository.AuthRepository
//import com.example.frontend.domain.usecase.LoginUseCase
//import com.example.frontend.domain.usecase.RegisterUseCase
//import com.example.frontend.presentation.screen.login.LoginScreen
//import com.example.frontend.presentation.screen.login.LoginViewModel
//import com.example.frontend.presentation.screen.register.RegisterScreen
//import com.example.frontend.presentation.screen.register.RegisterViewModel
//import com.example.frontend.ui.theme.FrontendTheme
//
//// ==========================================
//// 1. CÁC MOCK DÙNG CHUNG (SHARED MOCKS)
//// ==========================================
//
//// Fake Repository: Giả lập gọi API thành công cho cả Login và Register
//class FakeAuthRepository : AuthRepository {
//    override suspend fun getMe(): ApiResult<User> {
//        return ApiResult.Success(User("Preview User", "preview_user", null))
//    }
//
//    override suspend fun login(username: String, password: String): ApiResult<Unit> {
//        return ApiResult.Success(Unit)
//    }
//
//    override suspend fun logout() {}
//
//    override suspend fun register(email: String, password: String, mailOtp: String): ApiResult<Unit> {
//        return ApiResult.Success(Unit)
//    }
//}
//
//// Fake TokenProvider: Giả lập trạng thái Token (null = chưa đăng nhập)
//class FakeTokenProvider(private val hasToken: Boolean = false) : TokenProvider {
//    override suspend fun getAccessToken(): String? {
//        return if (hasToken) "fake_token_123" else null
//    }
//}
//
//// ==========================================
//// 2. PREVIEW MÀN HÌNH LOGIN
//// ==========================================
//@Preview(name = "Login - Light", showBackground = true)
//@Preview(name = "Login - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
//@Composable
//fun PreviewLoginScreen() {
//    // Setup Dependency thủ công
//    val repo = FakeAuthRepository()
//    val tokenProvider = FakeTokenProvider(hasToken = false) // Chưa có token để hiện form login
//    val loginUseCase = LoginUseCase(repo)
//    val viewModel = LoginViewModel(loginUseCase, tokenProvider)
//
//    FrontendTheme {
//        LoginScreen(
//            onLoggedIn = {},
//            onRegisterClick = {},
//            onForgotPasswordClick = {},
//            onGoogleLoginClick = {},
//            viewModel = viewModel
//        )
//    }
//}
//
//// ==========================================
//// 3. PREVIEW MÀN HÌNH REGISTER
//// ==========================================
//@Preview(name = "Register - Light", showBackground = true)
//@Preview(name = "Register - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
//@Composable
//fun PreviewRegisterScreen() {
//    // Setup Dependency thủ công
//    val repo = FakeAuthRepository()
//    val registerUseCase = RegisterUseCase(repo)
//    val viewModel = RegisterViewModel(registerUseCase)
//
//    FrontendTheme {
//        RegisterScreen(
//            onRegisterClick = {},
//            onLoginClick = {},
//            viewModel = viewModel
//        )
//    }
//}