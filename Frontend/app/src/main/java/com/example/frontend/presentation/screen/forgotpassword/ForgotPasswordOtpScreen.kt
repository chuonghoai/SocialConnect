package com.example.frontend.presentation.screen.forgotpassword

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ForgotPasswordOtpScreen(
    onBackClick: () -> Unit,
    onOtpVerified: (String, String) -> Unit,
    viewModel: ForgotPasswordOtpViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF6ED))
    ) {
        TopWaveDecoration()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .height(40.dp)
                        .background(Color.White.copy(alpha = 0.85f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFFF8A00)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Xác nhận OTP",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = Color(0xFF1E1E1E),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Nhập mã OTP đã gửi đến\n${state.email}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B6B6B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            OtpInputField(
                otpText = state.otp,
                onOtpChange = viewModel::updateOtp
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (state.canResend) {
                TextButton(
                    onClick = {
                        viewModel.resendOtp {
                            Toast.makeText(context, "Đã gửi lại OTP", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !state.loading
                ) {
                    Text("Gửi lại OTP", color = Color(0xFFFF8A00), fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text(
                    text = "Gửi lại OTP sau ${formatAsMinuteSecond(state.secondsUntilResend)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B6B6B)
                )
            }

            if (!state.error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            Button(
                onClick = {
                    viewModel.verifyOtp { email, otp ->
                        Toast.makeText(context, "OTP hợp lệ", Toast.LENGTH_SHORT).show()
                        onOtpVerified(email, otp)
                    }
                },
                enabled = !state.loading,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(52.dp),
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
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Xác nhận OTP",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

private fun formatAsMinuteSecond(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun OtpInputField(
    otpText: String,
    onOtpChange: (String) -> Unit
) {
    val focusRequesters = remember { List(6) { FocusRequester() } }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(6) { index ->
            val char = otpText.getOrNull(index)?.toString().orEmpty()

            OutlinedTextField(
                value = char,
                onValueChange = { input ->
                    val digit = input.filter { it.isDigit() }
                    if (digit.isEmpty()) {
                        val updated = otpText.toMutableList().apply {
                            if (index < size) removeAt(index)
                        }.joinToString("")
                        onOtpChange(updated)
                        return@OutlinedTextField
                    }

                    val current = otpText.padEnd(6, ' ').toCharArray()
                    current[index] = digit.last()
                    val updated = current.concatToString().replace(" ", "")
                    onOtpChange(updated)

                    if (index < 5) {
                        focusRequesters[index + 1].requestFocus()
                    }
                },
                modifier = Modifier
                    .width(45.dp)
                    .padding(horizontal = 4.dp)
                    .focusRequester(focusRequesters[index])
                    .onKeyEvent { keyEvent ->
                        if (
                            keyEvent.nativeKeyEvent.action == AndroidKeyEvent.ACTION_DOWN &&
                            keyEvent.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DEL
                        ) {
                            if (char.isEmpty() && index > 0) {
                                focusRequesters[index - 1].requestFocus()
                            }
                        }
                        false
                    },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = if (index == 5) ImeAction.Done else ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { if (index < 5) focusRequesters[index + 1].requestFocus() }
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF8A00),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = Color(0xFFFF8A00)
                )
            )
        }
    }
}

@Composable
private fun TopWaveDecoration() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
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
