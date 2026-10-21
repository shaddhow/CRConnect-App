package com.example.crconnect.student

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color Tokens
val Primary = Color(0xFF0062FF)
val PrimaryPressed = Color(0xFF0052CC)
val TintLightBlue = Color(0xFFEBF3FF)
val TintLightBlueBorder = Color(0xFFBFD7FF)
val BackgroundColor = Color(0xFFE9EEF4)
val TitleText = Color(0xFF0F172A)
val MutedText = Color(0xFF64748B)
val BorderColor = Color(0xFFE5E7EB)
val InputBorderColor = Color(0xFFCBD5E1)
val ErrorColor = Color(0xFFDC2626)
val SuccessColor = Color(0xFF16A34A)
val SuccessTint = Color(0xFFDCFCE7)
val SuccessText = Color(0xFF166534)
val SuccessBorder = Color(0xFFBBF7D0)
val WarningTint = Color(0xFFFFFBEB)
val WarningBorder = Color(0xFFFDE68A)
val WarningText = Color(0xFF92400E)
val DangerTint = Color(0xFFFEF2F2)
val DangerBorder = Color(0xFFFECACA)
val DisabledBg = Color(0xFFE5E7EB)
val DisabledText = Color(0xFF94A3B8)

val PlusJakartaSansFontFamily = FontFamily.SansSerif

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = TintLightBlue,
    onPrimaryContainer = Primary,
    background = BackgroundColor,
    onBackground = TitleText,
    surface = Color.White,
    onSurface = TitleText,
    surfaceTint = Color.Transparent,
    outline = BorderColor,
    error = ErrorColor
)

val StudentPortalTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.3).sp,
        color = TitleText
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.2).sp,
        color = TitleText
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.2).sp,
        color = TitleText
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = TitleText
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TitleText
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = TitleText
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = TitleText
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MutedText
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = TitleText
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MutedText
    )
)

val StudentPortalShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun StudentPortalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = StudentPortalTypography,
        shapes = StudentPortalShapes,
        content = content
    )
}
