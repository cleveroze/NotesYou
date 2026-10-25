@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.cleveroze.notesyou.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.cleveroze.notesyou.data.Settings
import io.github.cleveroze.notesyou.data.ThemeMode
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/** Seed colors offered in settings. The first one is the warm "wood" default. */
val SeedPresets = listOf(
    "Древесный" to "#8B5E3C",
    "Лаванда" to "#6750A4",
    "Мята" to "#3E8E6F",
    "Океан" to "#2F6FAB",
    "Коралл" to "#C8553D",
    "Роза" to "#B4487A",
    "Лимон" to "#A08A1F",
    "Графит" to "#5E6168",
)
const val DEFAULT_SEED = "#8B5E3C"

fun dynamicColorAvailable() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun NotesTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme = remember(settings.colorSource, dark) {
        if (settings.colorSource == "wallpaper" && dynamicColorAvailable()) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            val seed = if (settings.colorSource == "wallpaper") DEFAULT_SEED else settings.colorSource
            seedScheme(seed, dark)
        }
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        typography = AppTypography,
        content = content,
    )
}

private val base = Typography()
val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Light),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Medium),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

// ---------------------------------------------------------------------------
// Tonal palettes from a seed (CIE LCh — tone == L*, close to Material's HCT tone)
// ---------------------------------------------------------------------------

private fun lin(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
private fun delin(c: Double) = if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1 / 2.4) - 0.055
private fun fLab(t: Double) = if (t > 216.0 / 24389) cbrt(t) else (24389.0 / 27 * t + 16) / 116
private fun fInv(t: Double) = if (t * t * t > 216.0 / 24389) t * t * t else (116 * t - 16) / (24389.0 / 27)

private data class Lch(val l: Double, val c: Double, val h: Double)

private fun hexToLch(hex: String): Lch {
    val n = hex.removePrefix("#").toLong(16)
    val r = lin(((n shr 16) and 255) / 255.0)
    val g = lin(((n shr 8) and 255) / 255.0)
    val b = lin((n and 255) / 255.0)
    val x = 0.4124564 * r + 0.3575761 * g + 0.1804375 * b
    val y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b
    val z = 0.0193339 * r + 0.1191920 * g + 0.9503041 * b
    val fx = fLab(x / 0.95047); val fy = fLab(y); val fz = fLab(z / 1.08883)
    val a = 500 * (fx - fy); val bb = 200 * (fy - fz)
    var h = Math.toDegrees(atan2(bb, a)); if (h < 0) h += 360
    return Lch(116 * fy - 16, hypot(a, bb), h)
}

private fun lchToRgb(l: Double, c: Double, h: Double): DoubleArray {
    val a = c * cos(Math.toRadians(h)); val b = c * sin(Math.toRadians(h))
    val fy = (l + 16) / 116; val fx = fy + a / 500; val fz = fy - b / 200
    val x = 0.95047 * fInv(fx)
    val y = if (l > 8) fy * fy * fy else l / (24389.0 / 27)
    val z = 1.08883 * fInv(fz)
    return doubleArrayOf(
        3.2404542 * x - 1.5371385 * y - 0.4985314 * z,
        -0.9692660 * x + 1.8760108 * y + 0.0415560 * z,
        0.0556434 * x - 0.2040259 * y + 1.0572252 * z,
    )
}

private fun inGamut(rgb: DoubleArray) = rgb.all { it in -0.0005..1.0005 }

fun tone(hue: Double, chroma: Double, tone: Double): Color {
    val l = tone.coerceIn(0.0, 100.0)
    val h = ((hue % 360) + 360) % 360
    var rgb = lchToRgb(l, chroma, h)
    if (!inGamut(rgb)) {
        var lo = 0.0; var hi = chroma
        repeat(22) {
            val mid = (lo + hi) / 2
            if (inGamut(lchToRgb(l, mid, h))) lo = mid else hi = mid
        }
        rgb = lchToRgb(l, lo, h)
    }
    return Color(
        delin(rgb[0].coerceIn(0.0, 1.0)).toFloat(),
        delin(rgb[1].coerceIn(0.0, 1.0)).toFloat(),
        delin(rgb[2].coerceIn(0.0, 1.0)).toFloat(),
    )
}

/** Material "Expressive"-flavoured scheme: lively primary, a contrasting tertiary. */
fun seedScheme(seedHex: String, dark: Boolean): ColorScheme {
    val s = runCatching { hexToLch(seedHex) }.getOrDefault(hexToLch(DEFAULT_SEED))
    val p = { t: Double -> tone(s.h, maxOf(42.0, s.c), t) }
    val sc = { t: Double -> tone(s.h - 20, 22.0, t) }
    val tt = { t: Double -> tone(s.h + 75, 38.0, t) }
    val n = { t: Double -> tone(s.h, 7.0, t) }
    val nv = { t: Double -> tone(s.h, 13.0, t) }
    val e = { t: Double -> tone(25.0, 70.0, t) }
    return if (!dark) lightColorScheme(
        primary = p(40.0), onPrimary = p(100.0), primaryContainer = p(90.0), onPrimaryContainer = p(10.0),
        inversePrimary = p(80.0),
        secondary = sc(40.0), onSecondary = sc(100.0), secondaryContainer = sc(90.0), onSecondaryContainer = sc(10.0),
        tertiary = tt(40.0), onTertiary = tt(100.0), tertiaryContainer = tt(90.0), onTertiaryContainer = tt(10.0),
        error = e(40.0), onError = e(100.0), errorContainer = e(90.0), onErrorContainer = e(10.0),
        background = n(98.0), onBackground = n(10.0),
        surface = n(98.0), onSurface = n(10.0), surfaceVariant = nv(90.0), onSurfaceVariant = nv(30.0),
        surfaceTint = p(40.0), inverseSurface = n(20.0), inverseOnSurface = n(95.0),
        outline = nv(50.0), outlineVariant = nv(80.0), scrim = Color.Black,
        surfaceBright = n(98.0), surfaceDim = n(87.0),
        surfaceContainerLowest = n(100.0), surfaceContainerLow = n(96.0), surfaceContainer = n(94.0),
        surfaceContainerHigh = n(92.0), surfaceContainerHighest = n(90.0),
    ) else darkColorScheme(
        primary = p(80.0), onPrimary = p(20.0), primaryContainer = p(30.0), onPrimaryContainer = p(90.0),
        inversePrimary = p(40.0),
        secondary = sc(80.0), onSecondary = sc(20.0), secondaryContainer = sc(30.0), onSecondaryContainer = sc(90.0),
        tertiary = tt(80.0), onTertiary = tt(20.0), tertiaryContainer = tt(30.0), onTertiaryContainer = tt(90.0),
        error = e(80.0), onError = e(20.0), errorContainer = e(30.0), onErrorContainer = e(90.0),
        background = n(6.0), onBackground = n(90.0),
        surface = n(6.0), onSurface = n(90.0), surfaceVariant = nv(30.0), onSurfaceVariant = nv(80.0),
        surfaceTint = p(80.0), inverseSurface = n(90.0), inverseOnSurface = n(20.0),
        outline = nv(60.0), outlineVariant = nv(30.0), scrim = Color.Black,
        surfaceBright = n(24.0), surfaceDim = n(6.0),
        surfaceContainerLowest = n(4.0), surfaceContainerLow = n(10.0), surfaceContainer = n(12.0),
        surfaceContainerHigh = n(17.0), surfaceContainerHighest = n(22.0),
    )
}
