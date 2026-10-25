@file:OptIn(ExperimentalSharedTransitionApi::class)

package io.github.cleveroze.notesyou.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val LocalSharedScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavScope = compositionLocalOf<AnimatedVisibilityScope?> { null }
val LocalAnimations = compositionLocalOf { true }
val LocalHaptics = compositionLocalOf { true }

/** Container-transform between a note card and the editor. */
@Composable
fun Modifier.sharedNote(id: Long): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val nav = LocalNavScope.current ?: return this
    if (!LocalAnimations.current) return this
    return with(shared) {
        this@sharedNote.sharedBounds(
            rememberSharedContentState("note-$id"),
            animatedVisibilityScope = nav,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    }
}

/** Haptic tick that respects the setting. */
@Composable
fun rememberHaptic(): (Boolean) -> Unit {
    val h = LocalHapticFeedback.current
    val on = LocalHaptics.current
    return { strong -> if (on) h.performHapticFeedback(if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove) }
}

/** Highlights every case-insensitive match of [query]. */
fun highlight(text: String, query: String, bg: Color): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val q = query.trim().lowercase()
    val lower = text.lowercase()
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val j = lower.indexOf(q, i)
            if (j < 0) { append(text.substring(i)); break }
            append(text.substring(i, j))
            withStyle(SpanStyle(background = bg, fontWeight = FontWeight.Bold)) { append(text.substring(j, j + q.length)) }
            i = j + q.length
        }
    }
}

private val RU = Locale("ru")

fun friendlyDate(ms: Long, now: Long = System.currentTimeMillis()): String {
    val c = Calendar.getInstance().apply { timeInMillis = ms }
    val n = Calendar.getInstance().apply { timeInMillis = now }
    val sameDay = c.get(Calendar.YEAR) == n.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == n.get(Calendar.DAY_OF_YEAR)
    val time = SimpleDateFormat("HH:mm", RU).format(Date(ms))
    if (sameDay) return "сегодня, $time"
    n.add(Calendar.DAY_OF_YEAR, -1)
    if (c.get(Calendar.YEAR) == n.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == n.get(Calendar.DAY_OF_YEAR)) return "вчера, $time"
    n.add(Calendar.DAY_OF_YEAR, 2)
    if (c.get(Calendar.YEAR) == n.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == n.get(Calendar.DAY_OF_YEAR)) return "завтра, $time"
    val sameYear = c.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
    return SimpleDateFormat(if (sameYear) "d MMM, HH:mm" else "d MMM yyyy", RU).format(Date(ms))
}

fun plural(n: Int, one: String, few: String, many: String): String {
    val m10 = n % 10
    val m100 = n % 100
    return when {
        m10 == 1 && m100 != 11 -> one
        m10 in 2..4 && m100 !in 12..14 -> few
        else -> many
    }
}
