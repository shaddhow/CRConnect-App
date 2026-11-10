package com.example.crconnect.student

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
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

// Dark Theme Color Tokens - Deep Uniform Rich Dark Architecture
val DarkBackground = Color(0xFF0A0E1A)
val DarkSurface = Color(0xFF131B2E)
val DarkSurfaceVariant = Color(0xFF1C273E)
val DarkTitleText = Color(0xFFF8FAFC)
val DarkMutedText = Color(0xFF94A3B8)
val DarkBorderColor = Color(0xFF24354D)
val DarkInputBorderColor = Color(0xFF334155)

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
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = MutedText,
    surfaceTint = Color.Transparent,
    outline = BorderColor,
    error = ErrorColor
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFF60A5FA),
    background = DarkBackground,
    onBackground = DarkTitleText,
    surface = DarkSurface,
    onSurface = DarkTitleText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedText,
    surfaceTint = Color.Transparent,
    outline = DarkBorderColor,
    error = ErrorColor
)

val StudentPortalTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
)

val StudentPortalShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp)
)

fun getPortalGradient(darkTheme: Boolean): Brush {
    return if (darkTheme) {
        // Uniform deep rich dark background to eliminate vertical gradient banding & color depth mismatch
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0A0E1A),
                Color(0xFF0A0E1A)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE0F2FE), // Soft Sky Blue tint
                Color(0xFFF0F9FF), // Very light soft blue
                Color(0xFFF8FAFC)  // Clean off-white surface
            )
        )
    }
}

val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun StudentPortalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StudentPortalTypography,
            shapes = StudentPortalShapes,
            content = content
        )
    }
}
