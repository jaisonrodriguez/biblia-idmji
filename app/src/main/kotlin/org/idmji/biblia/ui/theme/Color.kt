package org.idmji.biblia.ui.theme

import androidx.compose.ui.graphics.Color

// Identidad IDMJI
val IdmjiPrimaryNavy = Color(0xFF0A2540)
val IdmjiNavyLight = Color(0xFF143B66)
val IdmjiGold = Color(0xFFC5A059)
val IdmjiGoldDark = Color(0xFF9E7835)

// Colores Tema Día
val DayBackground = Color(0xFFFCFAF7)
val DaySurface = Color(0xFFFFFFFF)
val DaySurfaceVariant = Color(0xFFF0EDE6)
val DayText = Color(0xFF1B1D1F)
val DayTextMuted = Color(0xFF6B7280)
val DayVerseNumber = Color(0xFF8B6C38)

// Colores Tema Sepia (Papel Biblia tradicional)
val SepiaBackground = Color(0xFFF5EFE0)
val SepiaSurface = Color(0xFFEBE3D0)
val SepiaSurfaceVariant = Color(0xFFE2D9C4)
val SepiaText = Color(0xFF2C2623)
val SepiaTextMuted = Color(0xFF6E645D)
val SepiaVerseNumber = Color(0xFF9E7535)

// Colores Tema Noche (Azul profundo de descanso)
val NightBackground = Color(0xFF0C1420)
val NightSurface = Color(0xFF162234)
val NightSurfaceVariant = Color(0xFF1E2E44)
val NightText = Color(0xFFE3E9F2)
val NightTextMuted = Color(0xFF8E9BAE)
val NightVerseNumber = Color(0xFFD4AF37)

// Resaltadores bíblicos
val HighlightYellow = Color(0xFFFFF3A8)
val HighlightGreen = Color(0xFFC8F7C5)
val HighlightBlue = Color(0xFFC5E3F6)
val HighlightPink = Color(0xFFFCD2D7)

fun parseHighlightColor(hex: String?): Color? {
    return when (hex) {
        "YELLOW" -> HighlightYellow
        "GREEN" -> HighlightGreen
        "BLUE" -> HighlightBlue
        "PINK" -> HighlightPink
        else -> null
    }
}
