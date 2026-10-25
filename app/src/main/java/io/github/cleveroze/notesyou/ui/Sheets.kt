@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package io.github.cleveroze.notesyou.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cleveroze.notesyou.data.Note
import io.github.cleveroze.notesyou.data.SortMode
import io.github.cleveroze.notesyou.data.ThemeMode
import io.github.cleveroze.notesyou.ui.components.MorphingShapeBox
import io.github.cleveroze.notesyou.ui.components.NoteShapes
import io.github.cleveroze.notesyou.ui.components.drawPolygon
import io.github.cleveroze.notesyou.ui.components.unit
import io.github.cleveroze.notesyou.ui.theme.NoteColors
import io.github.cleveroze.notesyou.ui.theme.SeedPresets
import io.github.cleveroze.notesyou.ui.theme.dynamicColorAvailable
import io.github.cleveroze.notesyou.ui.theme.noteSwatch
import io.github.cleveroze.notesyou.ui.theme.seedScheme
import java.util.Calendar

// ------------------------------------------------------------------------------------------
// Appearance: color + expressive shape badge
// ------------------------------------------------------------------------------------------

@Composable
fun AppearanceSheet(color: Int, shape: Int, onColor: (Int) -> Unit, onShape: (Int) -> Unit, onDismiss: () -> Unit) {
    var c by remember { mutableStateOf(color) }
    var s by remember { mutableStateOf(shape) }
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.4f
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Цвет", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NoteColors.forEachIndexed { i, nc ->
                    val sel = c == i
                    MorphingShapeBox(
                        target = if (sel) MaterialShapes.Cookie9Sided else MaterialShapes.Circle,
                        modifier = Modifier.size(48.dp).clickable { c = i; onColor(i) },
                        color = if (i == 0) MaterialTheme.colorScheme.surfaceContainerHighest else noteSwatch(i, dark),
                        spin = sel, spinMillis = 12_000,
                    ) {
                        when {
                            sel -> Icon(Icons.Rounded.Check, nc.name, tint = MaterialTheme.colorScheme.onSurface)
                            i == 0 -> Icon(Icons.Rounded.Block, nc.name, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Фигура", style = MaterialTheme.typography.titleMedium)
            Text("Значок Material 3 Expressive на карточке", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ShapeTile(selected = s == -1, onClick = { s = -1; onShape(-1) }) {
                    Icon(Icons.Rounded.Block, "Без фигуры", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                NoteShapes.all.forEachIndexed { i, (_, poly) ->
                    val unitPoly = remember(i) { poly.unit() }
                    val sel = s == i
                    val rot by animateFloatAsState(if (sel) 360f else 0f, MaterialTheme.motionScheme.slowSpatialSpec(), label = "r")
                    val accent = MaterialTheme.colorScheme.primary
                    val dim = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    ShapeTile(selected = sel, onClick = { s = i; onShape(i) }) {
                        Canvas(Modifier.size(32.dp).graphicsLayer { rotationZ = rot }) { drawPolygon(unitPoly, if (sel) accent else dim) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShapeTile(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val corner by androidx.compose.animation.core.animateDpAsState(if (selected) 28.dp else 16.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "c")
    Box(
        Modifier.size(56.dp).clip(RoundedCornerShape(corner))
            .background(if (selected) cs.primaryContainer else cs.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

// ------------------------------------------------------------------------------------------
// Labels
// ------------------------------------------------------------------------------------------

@Composable
fun LabelPickSheet(labels: List<String>, active: Set<String>, onToggle: (String) -> Unit, onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var newLabel by remember { mutableStateOf("") }
    var current by remember { mutableStateOf(active) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Ярлыки", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEach { l ->
                    val on = l in current
                    Surface(
                        onClick = { current = if (on) current - l else current + l; onToggle(l) },
                        shape = RoundedCornerShape(if (on) 20.dp else 10.dp),
                        color = if (on) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (on) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.Label, null, Modifier.size(18.dp))
                            Text(l, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newLabel, onValueChange = { newLabel = it.take(30) },
                    placeholder = { Text("Новый ярлык") }, singleLine = true,
                    shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    if (newLabel.isNotBlank()) { onAdd(newLabel.trim()); current = current + newLabel.trim(); newLabel = "" }
                }) { Icon(Icons.Rounded.Add, "Добавить") }
            }
        }
    }
}

@Composable
fun LabelsSheet(vm: NotesViewModel, labels: List<String>, onDismiss: () -> Unit) {
    var editing by remember { mutableStateOf<String?>(null) }
    var text by remember { mutableStateOf("") }
    var newLabel by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item { Text("Изменить ярлыки", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp)) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newLabel, onValueChange = { newLabel = it.take(30) }, placeholder = { Text("Создать ярлык") },
                        singleLine = true, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { vm.addLabel(newLabel); newLabel = "" }) { Icon(Icons.Rounded.Add, "Создать") }
                }
            }
            items(labels, key = { it }) { l ->
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.animateItem()) {
                    Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.Label, null)
                        if (editing == l) {
                            OutlinedTextField(
                                value = text, onValueChange = { text = it.take(30) }, singleLine = true,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            )
                            IconButton(onClick = { vm.renameLabel(l, text); editing = null }) { Icon(Icons.Rounded.Check, "Сохранить") }
                        } else {
                            Text(l, modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 12.dp))
                            IconButton(onClick = { editing = l; text = l }) { Icon(Icons.Rounded.Edit, "Переименовать") }
                            IconButton(onClick = { vm.deleteLabel(l) }) { Icon(Icons.Rounded.Delete, "Удалить") }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}

// ------------------------------------------------------------------------------------------
// Reminder
// ------------------------------------------------------------------------------------------

@Composable
fun ReminderSheet(current: Long?, onSet: (Long?) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf<Long?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(context, "Без разрешения уведомления не придут", Toast.LENGTH_LONG).show()
    }
    fun set(at: Long?) {
        if (at != null && Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        onSet(at)
        onDismiss()
    }
    fun at(daysAhead: Int, hour: Int, minute: Int = 0): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysAhead); set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0)
    }.timeInMillis
    val now = System.currentTimeMillis()
    val options = buildList {
        add("Через 15 минут" to now + 15 * 60_000)
        add("Через час" to now + 3_600_000)
        if (at(0, 18) > now + 10 * 60_000) add("Сегодня в 18:00" to at(0, 18))
        if (at(0, 21) > now + 10 * 60_000) add("Сегодня в 21:00" to at(0, 21))
        add("Завтра в 9:00" to at(1, 9))
        add("Через неделю" to at(7, 9))
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MorphingShapeBox(MaterialShapes.Sunny, Modifier.size(40.dp), color = MaterialTheme.colorScheme.tertiary, spinMillis = 9000)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Напомнить", style = MaterialTheme.typography.titleLarge)
                    if (current != null) Text("Сейчас: ${friendlyDate(current)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            options.forEach { (label, time) ->
                Surface(onClick = { set(time) }, shape = RoundedCornerShape(18.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
                        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(friendlyDate(time), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Surface(onClick = { pickDate = true }, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Выбрать дату и время…", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp))
            }
            if (current != null) TextButton(onClick = { set(null) }, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Rounded.Close, null); Text("Убрать напоминание", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    if (pickDate) {
        val ds = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = { TextButton(onClick = { pickTime = ds.selectedDateMillis ?: System.currentTimeMillis(); pickDate = false }) { Text("Далее") } },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Отмена") } },
        ) { DatePicker(ds) }
    }
    pickTime?.let { day ->
        val ts = rememberTimePickerState(initialHour = 9, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = null },
            title = { Text("Время") },
            text = { TimePicker(ts) },
            confirmButton = {
                TextButton(onClick = {
                    // DatePicker returns UTC midnight of the chosen day
                    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = day }
                    val local = Calendar.getInstance().apply {
                        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), ts.hour, ts.minute, 0)
                    }
                    pickTime = null
                    set(local.timeInMillis)
                }) { Text("Готово") }
            },
            dismissButton = { TextButton(onClick = { pickTime = null }) { Text("Отмена") } },
        )
    }
}

// ------------------------------------------------------------------------------------------
// Settings
// ------------------------------------------------------------------------------------------

@Composable
fun SettingsSheet(vm: NotesViewModel, onDismiss: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val update = vm::updateSettings
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) }
        }.onSuccess { Toast.makeText(context, "Заметки сохранены", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(context, "Не удалось сохранить", Toast.LENGTH_SHORT).show() }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            val r = text?.let { vm.importJson(it) }
            Toast.makeText(
                context,
                if (r?.isSuccess == true) "Импортировано: ${r.getOrNull()}" else "Файл не похож на резервную копию",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.4f
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { Text("Настройки", style = MaterialTheme.typography.headlineSmall) }
            item {
                Group("Цвет приложения") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (dynamicColorAvailable()) Swatch(
                            settings.colorSource == "wallpaper",
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondaryContainer),
                            "Обои",
                        ) { update { it.copy(colorSource = "wallpaper") } }
                        SeedPresets.forEach { (name, hex) ->
                            val sc = remember(hex) { seedScheme(hex, false) }
                            Swatch(settings.colorSource.equals(hex, true), listOf(sc.primary, sc.tertiary, sc.secondaryContainer), name) {
                                update { it.copy(colorSource = hex) }
                            }
                        }
                    }
                }
            }
            item { Group("Тема") { Segmented(listOf(ThemeMode.SYSTEM to "Авто", ThemeMode.LIGHT to "Светлая", ThemeMode.DARK to "Тёмная"), settings.themeMode) { v -> update { it.copy(themeMode = v) } } } }
            item { Group("Вид") { Segmented(listOf(true to "Сетка", false to "Список"), settings.grid) { v -> update { it.copy(grid = v) } } } }
            if (settings.grid) item { Group("Колонки") { Segmented(listOf(2 to "2", 3 to "3"), settings.columns) { v -> update { it.copy(columns = v) } } } }
            item {
                Group("Сортировка") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SortMode.entries.forEach { m ->
                            val on = settings.sort == m
                            Surface(
                                onClick = { update { it.copy(sort = m) } },
                                shape = RoundedCornerShape(if (on) 20.dp else 10.dp),
                                color = if (on) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) { Text(m.title, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge) }
                        }
                    }
                }
            }
            item {
                Group("Строк в предпросмотре: ${settings.previewLines}") {
                    Slider(value = settings.previewLines.toFloat(), onValueChange = { v -> update { it.copy(previewLines = v.toInt()) } }, valueRange = 2f..16f, steps = 13)
                }
            }
            item {
                Group("Размер текста в редакторе: ${settings.editorTextSize}") {
                    Slider(value = settings.editorTextSize.toFloat(), onValueChange = { v -> update { it.copy(editorTextSize = v.toInt()) } }, valueRange = 14f..28f, steps = 13)
                }
            }
            item {
                Group("Цвет новых заметок") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NoteColors.indices.forEach { i ->
                            val on = settings.defaultColor == i
                            Box(
                                Modifier.size(36.dp).clip(if (on) io.github.cleveroze.notesyou.ui.components.PolygonShape(MaterialShapes.Cookie6Sided) else CircleShape)
                                    .background(if (i == 0) MaterialTheme.colorScheme.surfaceContainerHighest else noteSwatch(i, dark))
                                    .clickable { update { it.copy(defaultColor = i) } },
                                contentAlignment = Alignment.Center,
                            ) { if (on) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                        }
                    }
                }
            }
            item { ToggleRow("Анимации", "Морфинг фигур, переходы, пружины", settings.animations) { v -> update { it.copy(animations = v) } } }
            item { ToggleRow("Вибрация", "Тактильный отклик", settings.haptics) { v -> update { it.copy(haptics = v) } } }
            item { ToggleRow("Смахнуть в архив", "Свайп по карточке отправляет в архив", settings.swipeToArchive) { v -> update { it.copy(swipeToArchive = v) } } }
            item { ToggleRow("Показывать даты", "Время изменения на карточках", settings.showDates) { v -> update { it.copy(showDates = v) } } }
            item { ToggleRow("Отмеченное вниз", "Выполненные пункты списка уходят вниз", settings.checkedToBottom) { v -> update { it.copy(checkedToBottom = v) } } }
            item {
                Group("Резервная копия") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionTile("Экспорт", Icons.Rounded.FileUpload, Modifier.weight(1f)) { export.launch("notes-backup.json") }
                        ActionTile("Импорт", Icons.Rounded.FileDownload, Modifier.weight(1f)) { import.launch(arrayOf("application/json", "text/plain", "*/*")) }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    MorphingShapeBox(MaterialShapes.Cookie12Sided, Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary, spinMillis = 12_000)
                    Column(Modifier.padding(start = 14.dp)) {
                        Text("Заметки · NotesYou", style = MaterialTheme.typography.titleMedium)
                        Text("Material 3 Expressive · RippleOS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ActionTile(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.tertiaryContainer, modifier = modifier) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null)
            Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 10.dp))
        content()
    }
}

@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (v, label) ->
            SegmentedButton(selected = v == selected, onClick = { onSelect(v) }, shape = SegmentedButtonDefaults.itemShape(i, options.size)) {
                Text(label, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.clickable { onChange(!checked) }.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked, onCheckedChange = onChange,
                thumbContent = if (checked) { { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) } } else { null },
            )
        }
    }
}

@Composable
private fun Swatch(selected: Boolean, colors: List<Color>, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
            MorphingShapeBox(
                target = if (selected) MaterialShapes.Cookie9Sided else MaterialShapes.Circle,
                modifier = Modifier.size(60.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                spin = selected, spinMillis = 16_000,
            )
            Box(Modifier.size(40.dp).clip(CircleShape)) {
                Column {
                    Box(Modifier.size(40.dp, 20.dp).background(colors[0]))
                    Row {
                        Box(Modifier.size(20.dp).background(colors[1]))
                        Box(Modifier.size(20.dp).background(colors[2]))
                    }
                }
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

// ------------------------------------------------------------------------------------------
// Statistics
// ------------------------------------------------------------------------------------------

@Composable
fun StatsSheet(notes: List<Note>, labels: List<String>, onDismiss: () -> Unit) {
    val live = notes.filter { !it.isTrashed }
    val words = live.sumOf { it.words() }
    val items = live.flatMap { it.items }
    val done = items.count { it.checked }
    val topLabel = labels.maxByOrNull { l -> live.count { l in it.labels } }
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.4f
    val week = 7L * 24 * 3600 * 1000
    val thisWeek = live.count { System.currentTimeMillis() - it.created < week }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Статистика", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${live.size}", plural(live.size, "заметка", "заметки", "заметок"), MaterialShapes.Cookie9Sided, Modifier.weight(1f))
                StatTile("$words", plural(words, "слово", "слова", "слов"), MaterialShapes.Clover4Leaf, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(if (items.isEmpty()) "—" else "${done * 100 / items.size}%", "пунктов выполнено", MaterialShapes.Sunny, Modifier.weight(1f))
                StatTile("$thisWeek", "за неделю", MaterialShapes.SoftBurst, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${live.count { it.pinned }}", "закреплено", MaterialShapes.Flower, Modifier.weight(1f))
                StatTile("${live.count { it.archived }}", "в архиве", MaterialShapes.Cookie4Sided, Modifier.weight(1f))
            }
            if (topLabel != null) {
                Spacer(Modifier.height(16.dp))
                Text("Любимый ярлык: $topLabel", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
            Text("Цвета", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val counts = NoteColors.indices.map { i -> live.count { it.color == i } }
            val total = counts.sum().coerceAtLeast(1)
            val defaultColor = MaterialTheme.colorScheme.surfaceContainerHighest
            Canvas(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(14.dp))) {
                var x = 0f
                counts.forEachIndexed { i, c ->
                    if (c > 0) {
                        val w = size.width * c / total
                        drawRect(if (i == 0) defaultColor else noteSwatch(i, dark), topLeft = Offset(x, 0f), size = Size(w, size.height))
                        x += w
                    }
                }
            }
            live.minByOrNull { it.created }?.let {
                Spacer(Modifier.height(16.dp))
                Text("Самая старая заметка: ${friendlyDate(it.created)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            live.maxByOrNull { it.words() }?.takeIf { it.words() > 0 }?.let {
                Text(
                    "Самая длинная: «${it.title.ifBlank { it.plain().take(24) }}» — ${it.words()} ${plural(it.words(), "слово", "слова", "слов")}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, shape: androidx.graphics.shapes.RoundedPolygon, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp), color = cs.surfaceContainerHigh, modifier = modifier) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            MorphingShapeBox(shape, Modifier.size(36.dp), color = cs.primaryContainer, spinMillis = 18_000)
            Column(Modifier.padding(start = 12.dp)) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Suppress("unused")
private fun Modifier.outline(color: Color) = this.border(1.dp, color, RoundedCornerShape(12.dp))
