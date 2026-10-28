package com.sanctuary.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val Sage = Color(0xFF5A7A67)
internal val SageLight = Color(0xFFA6C4B1)
internal val Oat = Color(0xFFF4F1E8)
internal val OatDeep = Color(0xFFE9E4D7)
internal val Ink = Color(0xFF1E2A24)
internal val Night = Color(0xFF121815)
internal val NightSurface = Color(0xFF19211D)
internal val Clay = Color(0xFFA3836A)
internal val Mist = Color(0xFF6E8B96)
internal val DustyRose = Color(0xFFA47A72)

@Immutable
data class SanctuaryColors(
    val glow: Color,
    val chartLine: Color,
    val chartFill: Color,
    val tiktok: Color,
    val instagram: Color,
    val youtube: Color,
    val success: Color,
    val warning: Color,
)

internal val LightSanctuaryColors = SanctuaryColors(
    glow = Color(0x336E8B96),
    chartLine = Sage,
    chartFill = Color(0x335A7A67),
    tiktok = Color(0xFF4F736E),
    instagram = DustyRose,
    youtube = Color(0xFF9B7652),
    success = Color(0xFF4E7A5E),
    warning = Color(0xFFA66F45),
)

internal val DarkSanctuaryColors = SanctuaryColors(
    glow = Color(0x2E8FAEA3),
    chartLine = SageLight,
    chartFill = Color(0x3DA6C4B1),
    tiktok = Color(0xFF8FB7AF),
    instagram = Color(0xFFC9A39A),
    youtube = Color(0xFFD0AE87),
    success = Color(0xFF93C2A2),
    warning = Color(0xFFD9A26F),
)

val LocalSanctuaryColors = staticCompositionLocalOf { LightSanctuaryColors }
