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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    onBackClick: () -> Unit = {},
    onRegisterClick: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val handleRegisterSuccess: () -> Unit = {
        Toast.makeText(context, "X\u00E1c th\u1EF1c OTP th\u00E0nh c\u00F4ng", Toast.LENGTH_SHORT).show()
        onRegisterClick()
    }

    LaunchedEffect(state.otp) {
        if (state.otp.length == 6) {
            viewModel.register(handleRegisterSuccess)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF6ED))
    ) {
        TopWaveDecoration()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

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

            Text(
                text = "X\u00E1c nh\u1EADn",
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

            OtpInputField(
                otpText = state.otp,
                onOtpChange = { newValue ->
                    if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                        viewModel.updateOtp(newValue)
                    }
                }
            )

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

            Button(
                onClick = { viewModel.register(handleRegisterSuccess) },
                enabled = state.otp.length == 6 && !state.loading,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF9C98A),
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
                        text = "X\u00E1c nh\u1EADn",
                        modifier = Modifier.padding(vertical = 2.dp),
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
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    val char = when {
                        index < otpText.length -> otpText[index].toString()
                        else -> ""
                    }

                    val isFocused = index == otpText.length

                    Box(
                        modifier = Modifier
                            .width(45.dp)
                            .height(50.dp)
                            .padding(horizontal = 4.dp)
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
