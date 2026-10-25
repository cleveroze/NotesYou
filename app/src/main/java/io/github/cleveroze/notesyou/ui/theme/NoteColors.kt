package io.github.cleveroze.notesyou.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Note color families: name, hue and chroma (tonal, like Material's custom colors). */
data class NoteColor(val name: String, val hue: Double, val chroma: Double)

val NoteColors = listOf(
    NoteColor("Без цвета", 0.0, 0.0),
    NoteColor("Клубника", 25.0, 55.0),
    NoteColor("Мандарин", 55.0, 60.0),
    NoteColor("Банан", 90.0, 60.0),
    NoteColor("Фисташка", 130.0, 45.0),
    NoteColor("Мята", 165.0, 38.0),
    NoteColor("Море", 220.0, 40.0),
    NoteColor("Черника", 275.0, 45.0),
    NoteColor("Сирень", 315.0, 42.0),
    NoteColor("Какао", 60.0, 22.0),
    NoteColor("Графит", 260.0, 6.0),
)

data class NoteTones(val container: Color, val onContainer: Color, val accent: Color, val onAccent: Color)

@Composable
fun noteTones(index: Int): NoteTones {
    val cs = MaterialTheme.colorScheme
    val dark = cs.surface.luminance() < 0.4f
    if (index <= 0 || index >= NoteColors.size) {
        return NoteTones(cs.surfaceContainerHigh, cs.onSurface, cs.primary, cs.onPrimary)
    }
    val c = NoteColors[index]
    return androidx.compose.runtime.remember(index, dark) { NoteTones(
        container = tone(c.hue, c.chroma, if (dark) 26.0 else 90.0),
        onContainer = tone(c.hue, c.chroma, if (dark) 92.0 else 12.0),
        accent = tone(c.hue, c.chroma + 10, if (dark) 78.0 else 42.0),
        onAccent = tone(c.hue, c.chroma, if (dark) 15.0 else 98.0),
    ) }
}

/** Swatch preview color independent of theme. */
fun noteSwatch(index: Int, dark: Boolean): Color =
    if (index <= 0) Color.Transparent
    else NoteColors[index].let { tone(it.hue, it.chroma, if (dark) 40.0 else 82.0) }
