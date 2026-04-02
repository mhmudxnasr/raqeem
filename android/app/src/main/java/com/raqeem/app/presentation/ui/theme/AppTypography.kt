package com.raqeem.app.presentation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.raqeem.app.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

val InterFont = GoogleFont("Inter")
val JetBrainsMonoFont = GoogleFont("JetBrains Mono")

val InterFamily = androidx.compose.ui.text.font.FontFamily(
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Bold),
)

val MonoFamily = androidx.compose.ui.text.font.FontFamily(
    Font(googleFont = JetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = JetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = JetBrainsMonoFont, fontProvider = provider, weight = FontWeight.SemiBold),
)

val RaqeemTypography = Typography(
    headlineLarge  = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 40.sp, color = AppColors.textPrimary),
    headlineMedium = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 28.sp, color = AppColors.textPrimary),
    headlineSmall  = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = AppColors.textPrimary),
    titleLarge     = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = AppColors.textPrimary),
    titleMedium    = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = AppColors.textPrimary),
    titleSmall     = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = AppColors.textPrimary),
    bodyLarge      = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, color = AppColors.textPrimary),
    bodyMedium     = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = AppColors.textPrimary),
    bodySmall      = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = AppColors.textSecondary),
    labelLarge     = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.08.em, color = AppColors.textMuted),
    labelMedium    = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = AppColors.textSecondary),
    labelSmall     = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 10.sp, color = AppColors.textMuted),
)

// Named text styles for financial and structural UI elements
object AppTypography {
    val sectionLabel = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.08.em,
        color = AppColors.textMuted,
    )
    val heroAmount = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        fontFeatureSettings = "tnum",
        color = AppColors.textPrimary,
    )
    val largeAmount = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        fontFeatureSettings = "tnum",
        color = AppColors.textPrimary,
    )
    val inlineAmount = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        fontFeatureSettings = "tnum",
        color = AppColors.textPrimary,
    )
}
