package com.example.frontend.presentation.screen.register

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.frontend.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onSendOtpClick: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF6ED))
    ) {
        TopWaveDecoration()

        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(50.dp))

            // App icon
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = "App icon",
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(18.dp))
            )

            Spacer(Modifier.height(18.dp))

            // Screen title
            Text(
                text = "Đăng ký",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color(0xFF1E1E1E),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            // Username
            RoundedInputField(
                value = state.email,
                onValueChange = viewModel::setEmail,
                placeholder = "Tên đăng nhập",
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                enabled = !state.loading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() })
            )

            Spacer(Modifier.height(14.dp))

            // Password
            RoundedInputField(
                value = state.password,
                onValueChange = viewModel::setPassword,
                placeholder = "Mật khẩu",
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                isPassword = true,
                enabled = !state.loading,
                modifier = Modifier.focusRequester(passwordFocusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            Spacer(Modifier.height(14.dp))

            // Confirm password
            RoundedInputField(
                value = state.confirmPassword,
                onValueChange = viewModel::setConfirmPassword,
                placeholder = "Nhập lại mật khẩu",
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                isPassword = true,
                enabled = !state.loading,
                modifier = Modifier.focusRequester(passwordFocusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    viewModel.sendOtp(onSendOtpClick)
                })
            )

            Spacer(Modifier.height(16.dp))

            if (!state.error.isNullOrBlank()) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
            }

            // "Bạn đã có tài khoản? Đăng nhập"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bạn đã có tài khoản. ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B6B6B)
                )
                TextButton(
                    onClick = onLoginClick,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Đăng nhập",
                        color = Color(0xFFFF8A00),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Register button
            Button(
                onClick = { viewModel.sendOtp(onSendOtpClick) },
                enabled = !state.loading,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF9C98A),
                    contentColor = Color.White
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
                        text = "Đăng ký",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // Social register (Google)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nút tròn nền trắng sau icon Google
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFEDEDED), CircleShape)
                        .clickable { /* ... */ },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.icon_google),
                        contentDescription = "Google",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoundedInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val shape = RoundedCornerShape(28.dp)
    var showPassword by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        readOnly = !enabled,
        placeholder = {
            Text(
                text = placeholder,
                color = Color(0xFFB8B8B8)
            )
        },
        leadingIcon = {
            CompositionLocalProvider(LocalContentColor provides Color(0xFFB8B8B8)) {
                leadingIcon()
            }
        },
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password"
                    )
                }
            }
        },
        shape = shape,
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFFF8A00),
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            cursorColor = Color(0xFFFF8A00)
        ),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )

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
