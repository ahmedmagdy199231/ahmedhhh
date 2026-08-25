package app.lovable.giant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBg = Color(0xFF0F172A)
val CardBg = Color(0xFF1E293B)
val PrimaryEmerald = Color(0xFF10B981)
val PrimaryEmeraldVariant = Color(0xFF059669)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val ErrorRed = Color(0xFFEF4444)
val BorderColor = Color(0xFF334155)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryEmerald,
    onPrimary = Color.White,
    primaryContainer = CardBg,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = CardBg,
    onSurface = TextPrimary,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun GiantTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
