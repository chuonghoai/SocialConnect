package com.example.frontend.presentation.screen.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.*
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

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onGoogleLoginClick: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.checkAlreadyLoggedIn(onLoggedIn)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF6ED))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { focusManager.clearFocus() }
            )
    ) {
        TopWaveDecoration()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconImage()
            Spacer(Modifier.height(58.dp))

            Text(
                text = "Đăng nhập",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color(0xFF1E1E1E),
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))

            RoundedInputField(
                value = state.username,
                onValueChange = viewModel::setUsername,
                placeholder = "Tên đăng nhập",
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                enabled = !state.loading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() })
            )
            Spacer(Modifier.height(14.dp))

            RoundedInputField(
                value = state.password,
                onValueChange = viewModel::setPassword,
                placeholder = "Mật khẩu",
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                isPassword = true,
                enabled = !state.loading,
                modifier = Modifier.focusRequester(passwordFocusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    viewModel.login(onLoggedIn)
                })
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Bạn không có tài khoản. ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B6B6B)
                    )
                    TextButton(
                        onClick = onRegisterClick,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Đăng ký",
                            color = Color(0xFFFF8A00),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                TextButton(
                    onClick = onForgotPasswordClick,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Quên mật khẩu",
                        color = Color(0xFFFF8A00),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(18.dp))

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(Modifier.height(10.dp))
            }

            Button(
                onClick = { viewModel.login(onLoggedIn) },
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
                        text = "Đăng nhập",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun IconImage() {
    androidx.compose.foundation.Image(
        painter = painterResource(id = R.drawable.app_icon),
        contentDescription = "App icon",
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(18.dp))
    )
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
