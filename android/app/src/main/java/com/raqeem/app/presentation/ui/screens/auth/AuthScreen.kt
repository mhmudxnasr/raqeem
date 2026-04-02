package com.raqeem.app.presentation.ui.screens.auth

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import com.raqeem.app.R
import com.raqeem.app.presentation.ui.components.SurfaceCard
import com.raqeem.app.presentation.ui.theme.AppColors
import com.raqeem.app.presentation.ui.theme.AppTypography
import com.raqeem.app.presentation.ui.theme.MonoFamily

@Composable
fun AuthScreen(
    message: String?,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onResetPassword: (String) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isSignUp by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bgBase)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_splash_logo),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Unspecified
            )
            Text(
                text = "Your ledger,\nwithout the noise.",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.Medium,
                ),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Sign in to restore balances, transactions, budgets, and goals across your devices.",
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.textSecondary,
            )
        }

        SurfaceCard(
            backgroundColor = AppColors.bgElevated,
            borderColor = AppColors.borderAccent.copy(alpha = 0.28f),
        ) {
            Text(
                text = "WHAT YOU GET",
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.textMuted,
            )
            OnboardingFeature(
                title = "Fast logging",
                subtitle = "Keyboard-first expense and income capture.",
            )
            OnboardingFeature(
                title = "Quiet analytics",
                subtitle = "Monthly trends, budgets, and AI summaries without clutter.",
            )
            OnboardingFeature(
                title = "Offline-first sync",
                subtitle = "Local changes stay safe and sync when the network is available.",
            )
        }

        if (message != null) {
            SurfaceCard(
                backgroundColor = if (message.contains("sent", ignoreCase = true)) {
                    AppColors.positiveBg
                } else {
                    AppColors.negativeBg
                },
                borderColor = if (message.contains("sent", ignoreCase = true)) {
                    AppColors.borderPositive
                } else {
                    AppColors.borderNegative
                },
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.contains("sent", ignoreCase = true)) AppColors.positive else AppColors.negative,
                )
            }
        }

        SurfaceCard(
            backgroundColor = AppColors.bgElevated,
            borderColor = AppColors.borderDefault,
        ) {
            Text(
                text = if (isSignUp) "CREATE ACCOUNT" else "SIGN IN",
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.textMuted,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AuthModeButton(
                    title = "Sign In",
                    selected = !isSignUp,
                    onClick = { isSignUp = false },
                    modifier = Modifier.weight(1f),
                )
                AuthModeButton(
                    title = "Create Account",
                    selected = isSignUp,
                    onClick = { isSignUp = true },
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(color = AppColors.borderSubtle, thickness = 0.5.dp)

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.borderAccent,
                    unfocusedBorderColor = AppColors.borderSubtle,
                    focusedContainerColor = AppColors.bgSubtle,
                    unfocusedContainerColor = AppColors.bgSubtle,
                ),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (isSignUp) "Create password" else "Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.borderAccent,
                    unfocusedBorderColor = AppColors.borderSubtle,
                    focusedContainerColor = AppColors.bgSubtle,
                    unfocusedContainerColor = AppColors.bgSubtle,
                ),
            )

            Button(
                onClick = {
                    if (isSignUp) {
                        onSignUp(email, password)
                    } else {
                        onSignIn(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.purple500,
                    contentColor = Color.White,
                ),
                enabled = email.isNotBlank() && password.isNotBlank(),
            ) {
                Text(if (isSignUp) "Create Account" else "Sign In")
            }

            if (!isSignUp) {
                TextButton(
                    onClick = { onResetPassword(email) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Send password reset email", color = AppColors.purple300)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No gradients. No noise. Just your numbers.",
            style = AppTypography.sectionLabel,
            color = AppColors.textMuted,
        )
    }
}

@Composable
private fun AuthModeButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) AppColors.purple500 else AppColors.bgSubtle,
            contentColor = if (selected) Color.White else AppColors.textSecondary,
        ),
    ) {
        Text(title)
    }
}

@Composable
private fun OnboardingFeature(
    title: String,
    subtitle: String,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .border(1.dp, AppColors.borderDefault, RoundedCornerShape(99.dp))
                .background(AppColors.bgSubtle, RoundedCornerShape(99.dp))
                .padding(horizontal = 5.dp, vertical = 5.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.textPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary,
            )
        }
    }
}
