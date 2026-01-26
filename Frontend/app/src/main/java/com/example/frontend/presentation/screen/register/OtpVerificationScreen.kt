package com.example.frontend.presentation.screen.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    onBackClick: () -> Unit = {},
    onRegisterClick: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    // Lấy state từ ViewModel (Giả sử bạn đã thêm otpCode vào UI State)
    val state by viewModel.uiState.collectAsState()

    // Tự động trigger hành động khi nhập đủ 6 số
    LaunchedEffect(state.otp) {
        if (state.otp.length == 6) {
            // Ẩn bàn phím để trải nghiệm tốt hơn
            // focusManager.clearFocus() -> Có thể thêm nếu cần
            viewModel.register(onRegisterClick)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF6ED)) // Màu nền giống RegisterScreen
    ) {
        // Decoration (Sử dụng lại hàm vẽ sóng từ file cũ)
        TopWaveDecoration()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- HEADER: Nút Back ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFFF8A00)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // --- TITLE ---
            Text(
                text = "Xác thực OTP",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color(0xFF1E1E1E),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Mã xác thực 6 số đã được gửi đến email\n${state.email}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B6B6B),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(50.dp))

            // --- OTP INPUT (6 Ô) ---
            OtpInputField(
                otpText = state.otp,
                onOtpChange = { newValue ->
                    // Chỉ cho phép nhập số và tối đa 6 ký tự
                    if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                        viewModel.updateOtp(newValue) // Cần thêm hàm này vào ViewModel
                    }
                }
            )

            // Hiển thị lỗi nếu có
            if (!state.error.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- CONFIRM BUTTON ---
            Button(
                onClick = { viewModel.register(onRegisterClick) },
                // Disable nút nếu chưa nhập đủ 6 số hoặc đang loading
                enabled = state.otp.length == 6 && !state.loading,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(52.dp)
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF9C98A), // Màu cam nhạt giống RegisterScreen
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor = Color(0xFFA0A0A0)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Xác nhận",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun OtpInputField(
    otpText: String,
    onOtpChange: (String) -> Unit
) {
    BasicTextField(
        value = otpText,
        onValueChange = onOtpChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.Center, // Căn giữa các ô
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    val char = when {
                        index < otpText.length -> otpText[index].toString()
                        else -> ""
                    }

                    // Xác định ô đang được focus (là ô tiếp theo cần nhập)
                    val isFocused = index == otpText.length

                    Box(
                        modifier = Modifier
                            .width(45.dp) // Kích thước mỗi ô
                            .height(50.dp)
                            .padding(horizontal = 4.dp) // Khoảng cách giữa các ô
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) Color(0xFFFF8A00) else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = Color(0xFF1E1E1E),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
}

// Cần copy lại hàm TopWaveDecoration từ RegisterScreen hoặc public nó ra dùng chung
// Để code chạy được độc lập, tôi để lại ở đây:
@Composable
private fun TopWaveDecoration() {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val w = size.width
        val h = size.height

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(w * 0.72f, 0f)
            cubicTo(
                w * 0.55f, h * 0.20f,
                w * 0.40f, h * 0.05f,
                w * 0.25f, h * 0.35f
            )
            cubicTo(
                w * 0.15f, h * 0.55f,
                w * 0.05f, h * 0.65f,
                0f, h * 0.72f
            )
            close()
        }

        drawPath(path = path, color = Color(0xFFFFD9B7))
    }
}