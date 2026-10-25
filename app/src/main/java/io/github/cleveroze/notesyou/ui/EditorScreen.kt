@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package io.github.cleveroze.notesyou.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cleveroze.notesyou.data.CheckItem
import io.github.cleveroze.notesyou.data.Note
import io.github.cleveroze.notesyou.ui.components.MorphingShapeBox
import io.github.cleveroze.notesyou.ui.components.NoteShapes
import io.github.cleveroze.notesyou.ui.components.PolygonShape
import io.github.cleveroze.notesyou.ui.theme.noteTones
import kotlinx.coroutines.launch

private enum class EditorSheet { None, Appearance, Labels, Reminder }

@Composable
fun EditorScreen(vm: NotesViewModel, noteId: Long) {
    val initial = remember(noteId) { vm.note(noteId) }
    if (initial == null) {
        LaunchedEffect(Unit) { vm.closeEditor() }
        return
    }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val labels by vm.labels.collectAsStateWithLifecycle()
    var note by remember(noteId) { mutableStateOf(initial) }
    val context = LocalContext.current
    val haptic = rememberHaptic()

    // ---- undo / redo history (coalesces fast typing) ----
    val history = remember(noteId) { mutableStateListOf(initial) }
    var hIndex by remember(noteId) { mutableIntStateOf(0) }
    var lastPush by remember(noteId) { mutableLongStateOf(0L) }

    fun commit(n: Note, record: Boolean = true) {
        note = n
        vm.save(n)
        if (!record) return
        val now = System.currentTimeMillis()
        while (history.lastIndex > hIndex) history.removeAt(history.lastIndex)
        if (now - lastPush < 700 && history.size > 1) history[history.lastIndex] = n
        else { history.add(n); if (history.size > 120) history.removeAt(0) }
        hIndex = history.lastIndex
        lastPush = now
    }
    fun undo() { if (hIndex > 0) { hIndex--; note = history[hIndex]; vm.save(note); lastPush = 0 } }
    fun redo() { if (hIndex < history.lastIndex) { hIndex++; note = history[hIndex]; vm.save(note); lastPush = 0 } }

    var sheet by remember { mutableStateOf(EditorSheet.None) }
    var menu by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf(false) }

    BackHandler { vm.closeEditor() }

    val tones = noteTones(note.color)
    val bg by animateColorAsState(tones.container, MaterialTheme.motionScheme.slowEffectsSpec(), label = "bg")
    val fg by animateColorAsState(tones.onContainer, MaterialTheme.motionScheme.slowEffectsSpec(), label = "fg")

    CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides fg) {
        Box(Modifier.fillMaxSize().sharedNote(noteId).background(bg)) {
            Column(Modifier.fillMaxSize().imePadding()) {
                // ---------- top bar ----------
                Row(Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.closeEditor() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
                    Spacer(Modifier.weight(1f))
                    val pinScale = remember { Animatable(1f) }
                    val scope = rememberCoroutineScope()
                    IconButton(onClick = {
                        haptic(false)
                        commit(note.copy(pinned = !note.pinned, archived = if (!note.pinned) false else note.archived), record = false)
                        scope.launch { pinScale.snapTo(0.6f); pinScale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 400f)) }
                    }) {
                        Icon(
                            if (note.pinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin, "Закрепить",
                            modifier = Modifier.graphicsLayer { scaleX = pinScale.value; scaleY = pinScale.value; rotationZ = if (note.pinned) 0f else 30f },
                        )
                    }
                    IconButton(onClick = { sheet = EditorSheet.Reminder }) {
                        Icon(Icons.Rounded.Alarm, "Напоминание", tint = if (note.reminderAt != null) tones.accent else fg)
                    }
                    IconButton(onClick = {
                        if (note.archived) commit(note.copy(archived = false), record = false)
                        else { vm.archive(listOf(note.id)); vm.closeEditor() }
                    }) { Icon(if (note.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive, "Архив") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Ещё") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(20.dp)) {
                            DropdownMenuItem(
                                text = { Text(if (note.isChecklist) "Сделать текстом" else "Сделать списком") },
                                leadingIcon = { Icon(if (note.isChecklist) Icons.Rounded.TextFields else Icons.Rounded.CheckBox, null) },
                                onClick = { menu = false; commit(toggleChecklist(note)) },
                            )
                            DropdownMenuItem(text = { Text("Дублировать") }, leadingIcon = { Icon(Icons.Rounded.LibraryAdd, null) }, onClick = {
                                menu = false; vm.duplicate(note.id)
                            })
                            DropdownMenuItem(text = { Text("Поделиться") }, leadingIcon = { Icon(Icons.Rounded.Share, null) }, onClick = {
                                menu = false
                                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, note.plain())
                                if (note.title.isNotBlank()) send.putExtra(Intent.EXTRA_SUBJECT, note.title)
                                context.startActivity(Intent.createChooser(send, "Поделиться заметкой"))
                            })
                            DropdownMenuItem(text = { Text("Копировать текст") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = {
                                menu = false
                                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Заметка", note.plain()))
                            })
                            DropdownMenuItem(text = { Text("Информация") }, leadingIcon = { Icon(Icons.Rounded.Info, null) }, onClick = { menu = false; info = true })
                            DropdownMenuItem(text = { Text("Удалить") }, leadingIcon = { Icon(Icons.Rounded.Delete, null) }, onClick = {
                                menu = false; vm.trash(listOf(note.id))
                            })
                        }
                    }
                }

                // ---------- content ----------
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                ) {
                    NoteShapes.get(note.shape)?.let { poly ->
                        MorphingShapeBox(
                            target = poly,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp).size(56.dp).clickable { sheet = EditorSheet.Appearance },
                            color = tones.accent,
                            spin = LocalAnimations.current,
                            spinMillis = 20_000,
                        )
                    }
                    PlainField(
                        value = note.title,
                        onChange = { commit(note.copy(title = it)) },
                        placeholder = "Заголовок",
                        style = MaterialTheme.typography.headlineSmall.copy(color = fg),
                        color = fg,
                        imeAction = ImeAction.Next,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (note.isChecklist) {
                        ChecklistEditor(note, settings.checkedToBottom, settings.editorTextSize, tones.accent, tones.onAccent, fg) { commit(it) }
                    } else {
                        PlainField(
                            value = note.text,
                            onChange = { commit(note.copy(text = it)) },
                            placeholder = "Заметка",
                            style = MaterialTheme.typography.bodyLarge.copy(color = fg, fontSize = settings.editorTextSize.sp, lineHeight = (settings.editorTextSize * 1.5).sp),
                            color = fg,
                            singleLine = false,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        note.reminderAt?.let { at ->
                            Chip("⏰ " + friendlyDate(at), tones.accent.copy(alpha = 0.18f), fg) { sheet = EditorSheet.Reminder }
                        }
                        note.labels.forEach { l -> Chip(l, fg.copy(alpha = 0.08f), fg) { sheet = EditorSheet.Labels } }
                    }
                    Spacer(Modifier.height(120.dp))
                }

                // ---------- bottom toolbar ----------
                Surface(
                    color = fg.copy(alpha = 0.06f), contentColor = fg,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                ) {
                    Row(Modifier.navigationBarsPadding().fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { sheet = EditorSheet.Appearance }) { Icon(Icons.Rounded.Palette, "Оформление") }
                        IconButton(onClick = { commit(toggleChecklist(note)) }) { Icon(if (note.isChecklist) Icons.Rounded.TextFields else Icons.Rounded.CheckBox, "Список") }
                        IconButton(onClick = { sheet = EditorSheet.Labels }) { Icon(Icons.AutoMirrored.Rounded.Label, "Ярлыки") }
                        Text(
                            "${friendlyDate(note.edited)} · ${note.words()} ${plural(note.words(), "слово", "слова", "слов")}",
                            style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp), maxLines = 1,
                        )
                        IconButton(onClick = { haptic(false); undo() }, enabled = hIndex > 0) { Icon(Icons.AutoMirrored.Rounded.Undo, "Отменить") }
                        IconButton(onClick = { haptic(false); redo() }, enabled = hIndex < history.lastIndex) { Icon(Icons.AutoMirrored.Rounded.Redo, "Повторить") }
                    }
                }
            }
        }
    }

    when (sheet) {
        EditorSheet.Appearance -> AppearanceSheet(
            color = note.color, shape = note.shape,
            onColor = { commit(note.copy(color = it)) },
            onShape = { commit(note.copy(shape = it)) },
            onDismiss = { sheet = EditorSheet.None },
        )
        EditorSheet.Labels -> LabelPickSheet(
            labels = labels, active = note.labels.toSet(),
            onToggle = { l -> commit(note.copy(labels = if (l in note.labels) note.labels - l else note.labels + l)) },
            onAdd = { l -> vm.addLabel(l); commit(note.copy(labels = (note.labels + l.trim()).distinct())) },
            onDismiss = { sheet = EditorSheet.None },
        )
        EditorSheet.Reminder -> ReminderSheet(
            current = note.reminderAt,
            onSet = { commit(note.copy(reminderAt = it), record = false) },
            onDismiss = { sheet = EditorSheet.None },
        )
        EditorSheet.None -> {}
    }

    if (info) AlertDialog(
        onDismissRequest = { info = false },
        confirmButton = { TextButton(onClick = { info = false }) { Text("Готово") } },
        icon = { MorphingShapeBox(MaterialShapes.Cookie9Sided, Modifier.size(40.dp), color = MaterialTheme.colorScheme.primary, spinMillis = 10_000) },
        title = { Text("О заметке") },
        text = {
            val plain = note.plain()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Создана: ${friendlyDate(note.created)}")
                Text("Изменена: ${friendlyDate(note.edited)}")
                Text("Слов: ${note.words()} · символов: ${plain.length}")
                Text("Строк: ${plain.lines().size}")
                if (note.isChecklist) Text("Выполнено: ${note.items.count { it.checked }} из ${note.items.size}")
                Text("Время чтения: ~${maxOf(1, note.words() / 180)} мин")
            }
        },
    )
}

private fun toggleChecklist(n: Note): Note =
    if (n.isChecklist) n.copy(isChecklist = false, text = n.items.joinToString("\n") { it.text }.trim(), items = emptyList())
    else n.copy(
        isChecklist = true,
        items = n.text.lines().filter { it.isNotBlank() }.ifEmpty { listOf("") }
            .mapIndexed { i, t -> CheckItem(i + 1L, t.trim().removePrefix("- ").removePrefix("• ")) },
        text = "",
    )

@Composable
private fun PlainField(
    value: String, onChange: (String) -> Unit, placeholder: String, style: TextStyle, color: Color,
    singleLine: Boolean = false, imeAction: ImeAction = ImeAction.Default, modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null, onNext: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxWidth()) {
        if (value.isEmpty()) Text(placeholder, style = style.copy(color = color.copy(alpha = 0.45f)))
        BasicTextField(
            value = value, onValueChange = onChange, textStyle = style, singleLine = singleLine,
            cursorBrush = SolidColor(color),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
            keyboardActions = KeyboardActions(onNext = { onNext?.invoke() }),
            modifier = Modifier.fillMaxWidth().let { if (focusRequester != null) it.focusRequester(focusRequester) else it },
        )
    }
}

@Composable
private fun Chip(text: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = bg, contentColor = fg, onClick = onClick) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun ChecklistEditor(
    note: Note, checkedToBottom: Boolean, textSize: Int, accent: Color, onAccent: Color, fg: Color,
    onChange: (Note) -> Unit,
) {
    val requesters = remember { mutableStateMapOf<Long, FocusRequester>() }
    var focusId by remember { mutableStateOf<Long?>(null) }
    var showChecked by remember { mutableStateOf(true) }
    val style = MaterialTheme.typography.bodyLarge.copy(color = fg, fontSize = textSize.sp)
    val nextId = { (note.items.maxOfOrNull { it.id } ?: 0L) + 1 }

    LaunchedEffect(focusId) { focusId?.let { id -> runCatching { requesters[id]?.requestFocus() } } }

    fun setItem(item: CheckItem) = onChange(note.copy(items = note.items.map { if (it.id == item.id) item else it }))
    fun addAfter(item: CheckItem?) {
        val n = CheckItem(nextId(), "")
        val idx = if (item == null) note.items.size else note.items.indexOfFirst { it.id == item.id } + 1
        onChange(note.copy(items = note.items.toMutableList().apply { add(idx.coerceIn(0, size), n) }))
        focusId = n.id
    }
    fun remove(item: CheckItem) = onChange(note.copy(items = note.items.filterNot { it.id == item.id }))

    val open = if (checkedToBottom) note.items.filter { !it.checked } else note.items
    val done = if (checkedToBottom) note.items.filter { it.checked } else emptyList()

    Column {
        open.forEach { item ->
            key(item.id) { ItemRow(item, style, accent, onAccent, fg, requesters.getOrPut(item.id) { FocusRequester() }, ::setItem, { addAfter(item) }, { remove(item) }) }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { addAfter(open.lastOrNull()) }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, null, Modifier.padding(start = 2.dp, end = 12.dp))
            Text("Пункт", style = style.copy(color = fg.copy(alpha = 0.6f)))
        }
        if (done.isNotEmpty()) {
            val rot by animateFloatAsState(if (showChecked) 0f else -90f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "rot")
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { showChecked = !showChecked }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.graphicsLayer { rotationZ = rot })
                Text("Отмечено: ${done.size}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
            }
            AnimatedVisibility(showChecked, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    done.forEach { item ->
                        key(item.id) { ItemRow(item, style, accent, onAccent, fg, requesters.getOrPut(item.id) { FocusRequester() }, ::setItem, { addAfter(item) }, { remove(item) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: CheckItem, style: TextStyle, accent: Color, onAccent: Color, fg: Color, focus: FocusRequester,
    onItem: (CheckItem) -> Unit, onNext: () -> Unit, onRemove: () -> Unit,
) {
    val haptic = rememberHaptic()
    val bounce = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Box(
            Modifier
                .size(26.dp)
                .graphicsLayer { scaleX = bounce.value; scaleY = bounce.value; rotationZ = if (item.checked) 45f * (2 - bounce.value) else 0f }
                .clip(if (item.checked) PolygonShape(MaterialShapes.Cookie4Sided) else RoundedCornerShape(8.dp))
                .background(if (item.checked) accent else Color.Transparent)
                .then(if (!item.checked) Modifier.border(2.dp, fg.copy(alpha = 0.5f), RoundedCornerShape(8.dp)) else Modifier)
                .clickable {
                    haptic(!item.checked)
                    onItem(item.copy(checked = !item.checked))
                    scope.launch { bounce.snapTo(0.5f); bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f)) }
                },
            contentAlignment = Alignment.Center,
        ) { if (item.checked) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = onAccent) }
        Spacer(Modifier.size(12.dp))
        Box(Modifier.weight(1f)) {
            PlainField(
                value = item.text,
                onChange = { onItem(item.copy(text = it.replace("\n", ""))) },
                placeholder = "Пункт",
                style = style.copy(
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                    color = if (item.checked) fg.copy(alpha = 0.5f) else fg,
                ),
                color = fg,
                singleLine = true,
                imeAction = ImeAction.Next,
                focusRequester = focus,
                onNext = onNext,
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.Close, "Удалить пункт", Modifier.size(18.dp)) }
    }
}
