package com.raqeem.app.presentation.ui.screens.lock

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.raqeem.app.presentation.ui.components.SurfaceCard
import com.raqeem.app.presentation.ui.theme.AppColors

@Composable
fun UnlockScreen(
    isBiometricEnabled: Boolean,
    hasPin: Boolean,
    message: String?,
    onUnlockWithBiometric: () -> Unit,
    onVerifyPin: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    var pin by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bgBase)
            .padding(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Unlock Raqeem",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "The app locked while it was in the background. Unlock to continue to your synced ledger.",
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.textSecondary,
        )

        message?.let { status ->
            SurfaceCard(
                backgroundColor = AppColors.negativeBg,
                borderColor = AppColors.borderNegative,
            ) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.negative,
                )
            }
        }

        if (isBiometricEnabled && context.supportsBiometric()) {
            Button(
                onClick = {
                    context.launchBiometricPrompt {
                        onUnlockWithBiometric()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.purple500,
                    contentColor = Color.White,
                ),
            ) {
                Text("Unlock with biometrics")
            }
        }

        if (hasPin) {
            SurfaceCard {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.take(6).filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("PIN") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.borderAccent,
                        unfocusedBorderColor = AppColors.borderSubtle,
                        focusedContainerColor = AppColors.bgSubtle,
                        unfocusedContainerColor = AppColors.bgSubtle,
                    ),
                )
                Button(
                    onClick = { onVerifyPin(pin) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.bgSubtle,
                        contentColor = AppColors.textPrimary,
                    ),
                ) {
                    Text("Unlock with PIN")
                }
            }
        }

        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.negativeBg,
                contentColor = AppColors.negative,
            ),
        ) {
            Text("Sign Out")
        }
    }
}

private fun Context.supportsBiometric(): Boolean {
    val biometricManager = BiometricManager.from(this)
    return biometricManager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
    ) == BiometricManager.BIOMETRIC_SUCCESS
}

private fun Context.launchBiometricPrompt(
    onSuccess: () -> Unit,
) {
    val activity = this as? FragmentActivity ?: return
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Raqeem")
        .setSubtitle("Confirm your identity to continue.")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
        .build()
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        },
    )
    prompt.authenticate(promptInfo)
}

