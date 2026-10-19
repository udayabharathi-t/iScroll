package app.lapse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val InstagramMark = Color(0xFFE7A2B5)
val YoutubeMark = Color(0xFFE85D4C)
val RedditMark = Color(0xFFFF8A4C)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE6B15A),
    onPrimary = Color(0xFF2A2112),
    secondary = Color(0xFFC5D2B4),
    onSecondary = Color(0xFF1A2116),
    background = Color(0xFF100E0C),
    onBackground = Color(0xFFF4EFE6),
    surface = Color(0xFF1B1814),
    onSurface = Color(0xFFF4EFE6),
    surfaceVariant = Color(0xFF26211C),
    onSurfaceVariant = Color(0xFFB7A99A),
    outline = Color(0xFF3A342C),
    error = Color(0xFFE85D4C),
    onError = Color(0xFF2A1210),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A5A12),
    onPrimary = Color(0xFFFFF8EE),
    secondary = Color(0xFF3E5340),
    onSecondary = Color(0xFFF4F7F1),
    background = Color(0xFFF4EFE6),
    onBackground = Color(0xFF1A1612),
    surface = Color(0xFFFFFBF6),
    onSurface = Color(0xFF1A1612),
    surfaceVariant = Color(0xFFE7DFD2),
    onSurfaceVariant = Color(0xFF5E564C),
    outline = Color(0xFFD9D0C4),
    error = Color(0xFFB33A2B),
    onError = Color(0xFFFFF8F6),
)

private val LapseType = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 64.sp,
        lineHeight = 68.sp,
        letterSpacing = (-1.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun LapseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = LapseType,
        content = content,
    )
}
