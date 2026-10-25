@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package io.github.cleveroze.notesyou.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cleveroze.notesyou.data.Filter
import io.github.cleveroze.notesyou.data.Note
import io.github.cleveroze.notesyou.ui.components.MorphingShapeBox
import io.github.cleveroze.notesyou.ui.components.NoteShapes
import io.github.cleveroze.notesyou.ui.components.PolygonShape
import io.github.cleveroze.notesyou.ui.theme.NoteColors
import io.github.cleveroze.notesyou.ui.theme.noteSwatch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class HomeSheet { None, Settings, Stats, Labels, Appearance, LabelPick }

@Composable
fun HomeScreen(vm: NotesViewModel) {
    val sections by vm.sections.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val labels by vm.labels.collectAsStateWithLifecycle()
    val undo by vm.undo.collectAsStateWithLifecycle()
    val colorFilter by vm.colorFilter.collectAsStateWithLifecycle()

    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var sheet by rememberSaveable { mutableStateOf(HomeSheet.None) }
    var fabOpen by rememberSaveable { mutableStateOf(false) }
    val haptic = rememberHaptic()
    val selecting = selection.isNotEmpty()

    BackHandler(selecting) { vm.clearSelection() }
    BackHandler(!selecting && drawer.isClosed && (filter != Filter.All || query.isNotEmpty() || colorFilter >= 0)) {
        vm.query.value = ""; vm.colorFilter.value = -1; vm.filter.value = Filter.All
    }
    BackHandler(fabOpen) { fabOpen = false }

    LaunchedEffect(undo?.id) {
        val e = undo ?: return@LaunchedEffect
        val r = snackbar.showSnackbar(e.message, actionLabel = if (e.snapshot.isNotEmpty()) "Отменить" else null, withDismissAction = true)
        if (r == SnackbarResult.ActionPerformed) vm.undoLast() else if (vm.undo.value?.id == e.id) vm.undo.value = null
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = !selecting,
        drawerContent = {
            Drawer(
                filter = filter, labels = labels,
                counts = vm.notes.collectAsStateWithLifecycle().value,
                onFilter = { vm.filter.value = it; vm.clearSelection(); scope.launch { drawer.close() } },
                onEditLabels = { sheet = HomeSheet.Labels; scope.launch { drawer.close() } },
                onStats = { sheet = HomeSheet.Stats; scope.launch { drawer.close() } },
                onSettings = { sheet = HomeSheet.Settings; scope.launch { drawer.close() } },
                onRandom = { scope.launch { drawer.close() }; vm.randomNote() },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                AnimatedContent(
                    targetState = selecting,
                    transitionSpec = { (fadeIn() + slideInVertically { -it / 2 }).togetherWith(fadeOut() + slideOutVertically { -it / 2 }) },
                    label = "topbar",
                ) { sel ->
                    if (sel) SelectionBar(vm, selection, filter, onColor = { sheet = HomeSheet.Appearance }, onLabel = { sheet = HomeSheet.LabelPick })
                    else SearchBarRow(
                        query = query,
                        onQuery = { vm.query.value = it },
                        title = when (val f = filter) {
                            Filter.All -> "Поиск заметок"
                            Filter.Reminders -> "Напоминания"
                            Filter.Archive -> "Архив"
                            Filter.Trash -> "Корзина"
                            is Filter.Label -> f.name
                        },
                        grid = settings.grid,
                        onMenu = { scope.launch { drawer.open() } },
                        onToggleGrid = { haptic(false); vm.updateSettings { it.copy(grid = !it.grid) } },
                        onSettings = { sheet = HomeSheet.Settings },
                    )
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = !selecting && filter != Filter.Trash && filter != Filter.Archive,
                    enter = scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    FabMenu(
                        open = fabOpen,
                        onToggle = { haptic(false); fabOpen = it },
                        onNote = { fabOpen = false; vm.create() },
                        onList = { fabOpen = false; vm.create(checklist = true) },
                        onRandomColor = { fabOpen = false; vm.create(color = (1 until NoteColors.size).random()) },
                        onReminder = { fabOpen = false; vm.create().let { id -> vm.note(id)?.let { n -> vm.save(n.copy(reminderAt = System.currentTimeMillis() + 3_600_000)) } } },
                    )
                }
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
                ColorFilterRow(colorFilter) { haptic(false); vm.colorFilter.value = if (vm.colorFilter.value == it) -1 else it }
                if (filter == Filter.Trash && sections.others.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Заметки удаляются через 30 дней", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.emptyTrash() }) { Text("Очистить") }
                    }
                }
                val motion = MaterialTheme.motionScheme
                AnimatedContent(
                    targetState = sections.isEmpty,
                    transitionSpec = { fadeIn(motion.slowEffectsSpec()).togetherWith(fadeOut()) },
                    label = "empty",
                ) { empty ->
                    if (empty) EmptyState(filter, query.isNotBlank() || colorFilter >= 0)
                    else NotesGrid(vm, sections.pinned, sections.others, settings, query, selection, filter, padding.calculateBottomPadding())
                }
            }
        }
    }

    when (sheet) {
        HomeSheet.Settings -> SettingsSheet(vm, onDismiss = { sheet = HomeSheet.None })
        HomeSheet.Stats -> StatsSheet(vm.notes.collectAsStateWithLifecycle().value, labels, onDismiss = { sheet = HomeSheet.None })
        HomeSheet.Labels -> LabelsSheet(vm, labels, onDismiss = { sheet = HomeSheet.None })
        HomeSheet.Appearance -> AppearanceSheet(
            color = vm.notes.value.firstOrNull { it.id in selection }?.color ?: 0,
            shape = vm.notes.value.firstOrNull { it.id in selection }?.shape ?: -1,
            onColor = { vm.setColor(selection, it) },
            onShape = { vm.setShape(selection, it) },
            onDismiss = { sheet = HomeSheet.None; vm.clearSelection() },
        )
        HomeSheet.LabelPick -> LabelPickSheet(
            labels = labels,
            active = vm.notes.value.filter { it.id in selection }.flatMap { it.labels }.toSet(),
            onToggle = { vm.toggleLabel(selection, it) },
            onAdd = { vm.addLabel(it); vm.toggleLabel(selection, it) },
            onDismiss = { sheet = HomeSheet.None; vm.clearSelection() },
        )
        HomeSheet.None -> {}
    }
}

@Composable
private fun SearchBarRow(
    query: String, onQuery: (String) -> Unit, title: String, grid: Boolean,
    onMenu: () -> Unit, onToggleGrid: () -> Unit, onSettings: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = cs.surfaceContainerHigh,
        modifier = Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().height(56.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onClick = onMenu) { Icon(Icons.Rounded.Menu, "Меню") }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) Text(title, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                BasicTextField(
                    value = query, onValueChange = onQuery, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                    cursorBrush = SolidColor(cs.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Close, "Очистить") }
            else Icon(Icons.Rounded.Search, null, tint = cs.onSurfaceVariant)
            IconButton(onClick = onToggleGrid) {
                AnimatedContent(grid, transitionSpec = { (scaleIn() + fadeIn()).togetherWith(scaleOut() + fadeOut()) }, label = "grid") { g ->
                    Icon(if (g) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView, "Вид")
                }
            }
            Box(
                Modifier.padding(end = 6.dp).size(40.dp).clip(PolygonShape(MaterialShapes.Cookie9Sided)).background(cs.primaryContainer)
                    .clickable(onClick = onSettings),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Settings, "Настройки", tint = cs.onPrimaryContainer, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun SelectionBar(vm: NotesViewModel, selection: Set<Long>, filter: Filter, onColor: () -> Unit, onLabel: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.primaryContainer, contentColor = cs.onPrimaryContainer, shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)) {
        Row(
            Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.clearSelection() }) { Icon(Icons.Rounded.Close, "Отменить выбор") }
            AnimatedContent(
                targetState = selection.size,
                transitionSpec = {
                    if (targetState > initialState) (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                    else (slideInVertically { -it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
                },
                label = "count",
            ) { n -> Text("$n", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp)) }
            Spacer(Modifier.weight(1f))
            val ids = selection
            if (filter == Filter.Trash) {
                IconButton(onClick = { vm.restore(ids) }) { Icon(Icons.Rounded.RestoreFromTrash, "Восстановить") }
                IconButton(onClick = { vm.deleteForever(ids) }) { Icon(Icons.Rounded.DeleteForever, "Удалить навсегда") }
            } else {
                IconButton(onClick = { vm.selectAll() }) { Icon(Icons.Rounded.SelectAll, "Выбрать все") }
                IconButton(onClick = { vm.togglePin(ids) }) { Icon(Icons.Rounded.PushPin, "Закрепить") }
                IconButton(onClick = onColor) { Icon(Icons.Rounded.Palette, "Оформление") }
                IconButton(onClick = onLabel) { Icon(Icons.AutoMirrored.Rounded.Label, "Ярлыки") }
                if (filter == Filter.Archive) IconButton(onClick = { vm.unarchive(ids) }) { Icon(Icons.Rounded.Unarchive, "Из архива") }
                else IconButton(onClick = { vm.archive(ids) }) { Icon(Icons.Rounded.Archive, "В архив") }
                IconButton(onClick = { vm.trash(ids) }) { Icon(Icons.Rounded.Delete, "Удалить") }
            }
        }
    }
}

@Composable
private fun ColorFilterRow(selected: Int, onPick: (Int) -> Unit) {
    val dark = MaterialTheme.colorScheme.surface.let { (it.red + it.green + it.blue) / 3 < 0.4f }
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (i in 1 until NoteColors.size) {
            val sel = selected == i
            val s by animateFloatAsState(if (sel) 1f else 0f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "c")
            Box(
                Modifier.size(30.dp).graphicsLayer { rotationZ = 45f * s }
                    .clip(if (sel) PolygonShape(MaterialShapes.Cookie4Sided) else CircleShape)
                    .background(noteSwatch(i, dark))
                    .then(if (sel) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, PolygonShape(MaterialShapes.Cookie4Sided)) else Modifier)
                    .clickable { onPick(i) },
            )
        }
    }
}

@Composable
private fun NotesGrid(
    vm: NotesViewModel, pinned: List<Note>, others: List<Note>,
    settings: io.github.cleveroze.notesyou.data.Settings, query: String, selection: Set<Long>, filter: Filter, bottom: androidx.compose.ui.unit.Dp,
) {
    val state = rememberLazyStaggeredGridState()
    val haptic = rememberHaptic()
    val cols = if (settings.grid) settings.columns else 1
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(cols),
        state = state,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = bottom + 96.dp),
        verticalItemSpacing = 10.dp,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        fun header(key: String, text: String) {
            item(key = key, span = StaggeredGridItemSpan.FullLine) {
                Text(
                    text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.animateItem().padding(start = 8.dp, top = 8.dp, bottom = 2.dp),
                )
            }
        }
        fun notes(list: List<Note>) {
            items(list, key = { it.id }) { note ->
                val swipe = settings.swipeToArchive && (filter == Filter.All || filter is Filter.Label) && selection.isEmpty()
                Box(Modifier.animateItem()) {
                    val card: @Composable () -> Unit = {
                        NoteCard(
                            note = note, query = query,
                            selected = note.id in selection, selectionMode = selection.isNotEmpty(),
                            settings = settings,
                            onClick = { if (selection.isNotEmpty()) { haptic(false); vm.toggleSelect(note.id) } else vm.open(note.id) },
                            onLongClick = { haptic(true); vm.toggleSelect(note.id) },
                            modifier = Modifier.fillMaxWidth().sharedNote(note.id),
                        )
                    }
                    if (swipe) SwipeArchive(onArchive = { haptic(true); vm.archive(listOf(note.id)) }) { card() } else card()
                }
            }
        }
        if (pinned.isNotEmpty()) {
            header("h-pinned", "Закреплённые")
            notes(pinned)
            if (others.isNotEmpty()) header("h-others", "Другие")
        }
        notes(others)
    }
}

@Composable
private fun SwipeArchive(onArchive: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) onArchive()
    }
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val progress = state.progress
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f + 0.7f * progress)),
                contentAlignment = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                MorphingShapeBox(
                    target = if (progress > 0.5f) MaterialShapes.Cookie9Sided else MaterialShapes.Circle,
                    modifier = Modifier.padding(horizontal = 20.dp).size(48.dp).graphicsLayer { val s = 0.6f + 0.4f * progress; scaleX = s; scaleY = s },
                    color = MaterialTheme.colorScheme.tertiary,
                ) { Icon(Icons.Rounded.Archive, null, tint = MaterialTheme.colorScheme.onTertiary) }
            }
        },
    ) { content() }
}

@Composable
private fun FabMenu(
    open: Boolean, onToggle: (Boolean) -> Unit,
    onNote: () -> Unit, onList: () -> Unit, onRandomColor: () -> Unit, onReminder: () -> Unit,
) {
    FloatingActionButtonMenu(
        expanded = open,
        button = {
            ToggleFloatingActionButton(checked = open, onCheckedChange = onToggle) {
                Icon(
                    Icons.Rounded.Add, "Создать",
                    modifier = Modifier.graphicsLayer { rotationZ = 135f * checkedProgress },
                )
            }
        },
    ) {
        FloatingActionButtonMenuItem(onClick = onReminder, icon = { Icon(Icons.Rounded.Alarm, null) }, text = { Text("С напоминанием") })
        FloatingActionButtonMenuItem(onClick = onRandomColor, icon = { Icon(Icons.Rounded.Palette, null) }, text = { Text("Цветная заметка") })
        FloatingActionButtonMenuItem(onClick = onList, icon = { Icon(Icons.Rounded.CheckBox, null) }, text = { Text("Список") })
        FloatingActionButtonMenuItem(onClick = onNote, icon = { Icon(Icons.Rounded.TextFields, null) }, text = { Text("Заметка") })
    }
}

@Composable
private fun EmptyState(filter: Filter, searching: Boolean) {
    var idx by remember { mutableStateOf(0) }
    val anim = LocalAnimations.current
    LaunchedEffect(anim) {
        while (anim) { delay(1800); idx = (idx + 1) % NoteShapes.cycle.size }
    }
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        MorphingShapeBox(
            target = NoteShapes.cycle[idx],
            modifier = Modifier.size(160.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            spin = anim,
            spinMillis = 16_000,
        ) {
            Icon(
                when {
                    searching -> Icons.Rounded.Search
                    filter == Filter.Archive -> Icons.Rounded.Archive
                    filter == Filter.Trash -> Icons.Rounded.Delete
                    filter == Filter.Reminders -> Icons.Rounded.Alarm
                    else -> Icons.Rounded.Lightbulb
                },
                null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            when {
                searching -> "Ничего не нашлось"
                filter == Filter.Archive -> "Архив пуст"
                filter == Filter.Trash -> "Корзина пуста"
                filter == Filter.Reminders -> "Напоминаний пока нет"
                filter is Filter.Label -> "С этим ярлыком заметок нет"
                else -> "Здесь появятся ваши заметки"
            },
            style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
        )
        Text(
            if (searching) "Попробуйте другой запрос или цвет" else "Нажмите «+», чтобы записать мысль",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun Drawer(
    filter: Filter, labels: List<String>, counts: List<Note>,
    onFilter: (Filter) -> Unit, onEditLabels: () -> Unit, onStats: () -> Unit, onSettings: () -> Unit, onRandom: () -> Unit,
) {
    ModalDrawerSheet(drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                MorphingShapeBox(MaterialShapes.Cookie12Sided, Modifier.size(44.dp), color = MaterialTheme.colorScheme.primary, spinMillis = 14_000) {
                    Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
                Text("Заметки", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 14.dp))
            }
            val active = counts.filter { !it.isTrashed }
            DrawerItem("Заметки", Icons.Rounded.Lightbulb, filter == Filter.All, active.count { !it.archived }) { onFilter(Filter.All) }
            DrawerItem("Напоминания", Icons.Rounded.Alarm, filter == Filter.Reminders, active.count { it.reminderAt != null }) { onFilter(Filter.Reminders) }
            if (labels.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                Text("Ярлыки", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, bottom = 4.dp))
                labels.forEach { l ->
                    DrawerItem(l, Icons.AutoMirrored.Rounded.Label, filter == Filter.Label(l), active.count { l in it.labels }) { onFilter(Filter.Label(l)) }
                }
            }
            DrawerItem("Изменить ярлыки", Icons.Rounded.Edit, false, null, onEditLabels)
            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            DrawerItem("Архив", Icons.Rounded.Archive, filter == Filter.Archive, active.count { it.archived }) { onFilter(Filter.Archive) }
            DrawerItem("Корзина", Icons.Rounded.Delete, filter == Filter.Trash, counts.count { it.isTrashed }) { onFilter(Filter.Trash) }
            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            DrawerItem("Случайная заметка", Icons.Rounded.Shuffle, false, null, onRandom)
            DrawerItem("Статистика", Icons.Rounded.BarChart, false, null, onStats)
            DrawerItem("Настройки", Icons.Rounded.Settings, false, null, onSettings)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DrawerItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, count: Int?, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, null) },
        badge = { if (count != null && count > 0) Text("$count") },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 1.dp),
    )
}
