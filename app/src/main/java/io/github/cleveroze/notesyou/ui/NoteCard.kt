@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.cleveroze.notesyou.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.cleveroze.notesyou.data.Note
import io.github.cleveroze.notesyou.data.Settings
import io.github.cleveroze.notesyou.ui.components.MorphingShapeBox
import io.github.cleveroze.notesyou.ui.components.NoteShapes
import io.github.cleveroze.notesyou.ui.components.drawPolygon
import io.github.cleveroze.notesyou.ui.components.unit
import io.github.cleveroze.notesyou.ui.theme.noteTones

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    note: Note,
    query: String,
    selected: Boolean,
    selectionMode: Boolean,
    settings: Settings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tones = noteTones(note.color)
    val cs = MaterialTheme.colorScheme
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val corner by animateDpAsState(if (selected) 36.dp else 24.dp, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "corner")
    val scale by animateFloatAsState(if (selected) 0.95f else 1f, spatial, label = "scale")
    val border by animateColorAsState(if (selected) cs.primary else Color.Transparent, label = "border")
    val shape = RoundedCornerShape(corner)
    val hl = tones.accent.copy(alpha = 0.3f)

    Surface(
        shape = shape,
        color = tones.container,
        contentColor = tones.onContainer,
        border = if (selected) BorderStroke(3.dp, border) else if (note.color == 0) BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.6f)) else null,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box {
            Column(Modifier.padding(16.dp)) {
                if (note.shape >= 0 || note.pinned || note.reminderAt != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        NoteShapes.get(note.shape)?.let { poly ->
                            val unitPoly = remember(note.shape) { poly.unit() }
                            Canvas(Modifier.size(26.dp)) { drawPolygon(unitPoly, tones.accent) }
                            Spacer(Modifier.width(8.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        if (note.reminderAt != null) Icon(Icons.Rounded.Alarm, null, Modifier.size(18.dp), tint = tones.accent)
                        if (note.pinned) Icon(Icons.Rounded.PushPin, null, Modifier.padding(start = 4.dp).size(18.dp), tint = tones.accent)
                    }
                }
                if (note.title.isNotBlank()) {
                    Text(
                        highlight(note.title, query, hl),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 3, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                if (note.isChecklist) {
                    val items = if (settings.checkedToBottom) note.items.sortedBy { it.checked } else note.items
                    items.filter { it.text.isNotBlank() }.take(settings.previewLines).forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Box(
                                Modifier.size(16.dp).clip(
                                    if (item.checked) io.github.cleveroze.notesyou.ui.components.PolygonShape(MaterialShapes.Cookie4Sided)
                                    else RoundedCornerShape(5.dp)
                                ).background(if (item.checked) tones.accent else tones.onContainer.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (item.checked) Icon(Icons.Rounded.Check, null, Modifier.size(12.dp), tint = tones.onAccent)
                            }
                            Text(
                                highlight(item.text, query, hl),
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                                color = if (item.checked) tones.onContainer.copy(alpha = 0.55f) else tones.onContainer,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                    val done = note.items.count { it.checked }
                    if (note.items.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        ChecklistProgress(done, note.items.size, tones.accent, tones.onContainer.copy(alpha = 0.12f))
                    }
                } else if (note.text.isNotBlank()) {
                    Text(
                        highlight(note.text, query, hl),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = settings.previewLines, overflow = TextOverflow.Ellipsis,
                        color = tones.onContainer.copy(alpha = 0.86f),
                    )
                }
                if (note.labels.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        note.labels.take(4).forEach {
                            Surface(shape = RoundedCornerShape(10.dp), color = tones.onContainer.copy(alpha = 0.08f)) {
                                Text(it, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
                if (note.reminderAt != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("⏰ " + friendlyDate(note.reminderAt), style = MaterialTheme.typography.labelSmall, color = tones.accent)
                } else if (settings.showDates) {
                    Spacer(Modifier.height(8.dp))
                    Text(friendlyDate(note.edited), style = MaterialTheme.typography.labelSmall, color = tones.onContainer.copy(alpha = 0.5f))
                }
            }
            // selection badge: a circle that morphs into a cookie with a check
            if (selectionMode) {
                MorphingShapeBox(
                    target = if (selected) MaterialShapes.Cookie9Sided else MaterialShapes.Circle,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp),
                    color = if (selected) cs.primary else cs.surfaceContainerHighest.copy(alpha = 0.9f),
                    spin = selected,
                    spinMillis = 8000,
                ) {
                    if (selected) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = cs.onPrimary)
                }
            }
        }
    }
}

@Composable
fun ChecklistProgress(done: Int, total: Int, color: Color, track: Color) {
    val p by animateFloatAsState(if (total == 0) 0f else done.toFloat() / total, MaterialTheme.motionScheme.slowSpatialSpec(), label = "p")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.weight(1f).height(6.dp)) {
            val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
            drawRoundRect(track, cornerRadius = r)
            drawRoundRect(color, size = size.copy(width = (size.width * p).coerceAtLeast(size.height)), cornerRadius = r)
        }
        Text("$done/$total", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))
    }
}
