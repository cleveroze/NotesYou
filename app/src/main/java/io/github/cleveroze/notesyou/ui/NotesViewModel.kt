package io.github.cleveroze.notesyou.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.cleveroze.notesyou.data.CheckItem
import io.github.cleveroze.notesyou.data.Filter
import io.github.cleveroze.notesyou.data.Note
import io.github.cleveroze.notesyou.data.NotesStore
import io.github.cleveroze.notesyou.data.Prefs
import io.github.cleveroze.notesyou.data.Settings
import io.github.cleveroze.notesyou.data.SortMode
import io.github.cleveroze.notesyou.reminders.Reminders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class Sections(val pinned: List<Note>, val others: List<Note>) {
    val isEmpty get() = pinned.isEmpty() && others.isEmpty()
    val all get() = pinned + others
}

data class UndoEvent(val message: String, val snapshot: List<Note>, val id: Long = System.nanoTime())

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val store = NotesStore(app)
    private val prefs = Prefs(app)

    val notes: StateFlow<List<Note>> = store.notes
    val labels: StateFlow<List<String>> = store.labels
    val settings: StateFlow<Settings> = prefs.settings

    val filter = MutableStateFlow<Filter>(Filter.All)
    val query = MutableStateFlow("")
    val selection = MutableStateFlow<Set<Long>>(emptySet())
    val openNoteId = MutableStateFlow<Long?>(null)
    val undo = MutableStateFlow<UndoEvent?>(null)
    /** Colour filter chip on the home screen; -1 = all colours */
    val colorFilter = MutableStateFlow(-1)

    private val queryState = combine(query, colorFilter) { q, c -> q to c }

    val sections: StateFlow<Sections> = combine(store.notes, filter, queryState, prefs.settings) { all, f, qc, s ->
        val (q, colour) = qc
        var list = when (f) {
            Filter.All -> all.filter { !it.archived && !it.isTrashed }
            Filter.Reminders -> all.filter { it.reminderAt != null && !it.isTrashed }
            Filter.Archive -> all.filter { it.archived && !it.isTrashed }
            Filter.Trash -> all.filter { it.isTrashed }
            is Filter.Label -> all.filter { f.name in it.labels && !it.isTrashed }
        }
        if (colour >= 0) list = list.filter { it.color == colour }
        if (q.isNotBlank()) {
            val needle = q.trim().lowercase()
            list = list.filter { n -> n.plain().lowercase().contains(needle) || n.labels.any { it.lowercase().contains(needle) } }
        }
        list = when (s.sort) {
            SortMode.EDITED -> list.sortedByDescending { it.edited }
            SortMode.CREATED -> list.sortedByDescending { it.created }
            SortMode.TITLE -> list.sortedBy { it.title.ifBlank { it.plain() }.lowercase() }
            SortMode.COLOR -> list.sortedWith(compareBy<Note> { if (it.color == 0) Int.MAX_VALUE else it.color }.thenByDescending { it.edited })
            SortMode.REMINDER -> list.sortedBy { it.reminderAt ?: Long.MAX_VALUE }
        }
        if (f == Filter.Trash || f == Filter.Archive) Sections(emptyList(), list)
        else Sections(list.filter { it.pinned }, list.filterNot { it.pinned })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Sections(emptyList(), emptyList()))

    fun note(id: Long): Note? = store.get(id)

    // ---------------- navigation ----------------
    fun open(id: Long) { openNoteId.value = id }

    fun closeEditor() {
        val id = openNoteId.value ?: return
        openNoteId.value = null
        val n = store.get(id) ?: return
        if (n.isBlank && n.reminderAt == null) {
            store.deleteForever(listOf(id))
            undo.value = UndoEvent("Пустая заметка удалена", emptyList())
        }
    }

    fun create(checklist: Boolean = false, color: Int? = null, label: String? = (filter.value as? Filter.Label)?.name): Long {
        val id = store.newId()
        store.upsert(
            Note(
                id = id,
                isChecklist = checklist,
                items = if (checklist) listOf(CheckItem(1, "")) else emptyList(),
                color = color ?: settings.value.defaultColor,
                labels = listOfNotNull(label),
            )
        )
        open(id)
        return id
    }

    // ---------------- editing ----------------
    fun save(note: Note, touch: Boolean = true) {
        val old = store.get(note.id)
        val n = if (touch) note.copy(edited = System.currentTimeMillis()) else note
        store.upsert(n)
        if (old?.reminderAt != n.reminderAt || old?.title != n.title) Reminders.schedule(getApplication(), n)
    }

    private fun snapshot(ids: Collection<Long>) = notes.value.filter { it.id in ids }

    private fun bulk(ids: Collection<Long>, message: String?, edit: (Note) -> Note) {
        if (ids.isEmpty()) return
        val before = snapshot(ids)
        store.update(ids, edit)
        if (message != null) undo.value = UndoEvent(message, before)
        selection.value = emptySet()
    }

    fun togglePin(ids: Collection<Long>) {
        val pinAll = snapshot(ids).any { !it.pinned }
        bulk(ids, null) { it.copy(pinned = pinAll, archived = if (pinAll) false else it.archived) }
    }

    fun archive(ids: Collection<Long>) = bulk(ids, if (ids.size == 1) "Заметка в архиве" else "Заметок в архиве: ${ids.size}") {
        it.copy(archived = true, pinned = false)
    }

    fun unarchive(ids: Collection<Long>) = bulk(ids, "Возвращено из архива") { it.copy(archived = false) }

    fun trash(ids: Collection<Long>) {
        snapshot(ids).forEach { Reminders.cancel(getApplication(), it) }
        bulk(ids, if (ids.size == 1) "Заметка в корзине" else "В корзине: ${ids.size}") { it.copy(trashedAt = System.currentTimeMillis(), pinned = false) }
        if (openNoteId.value?.let { it in ids } == true) openNoteId.value = null
    }

    fun restore(ids: Collection<Long>) = bulk(ids, "Восстановлено") { it.copy(trashedAt = null) }

    fun deleteForever(ids: Collection<Long>) {
        store.deleteForever(ids)
        selection.value = emptySet()
    }

    fun emptyTrash() = store.emptyTrash()

    fun setColor(ids: Collection<Long>, color: Int) = bulk(ids, null) { it.copy(color = color) }

    fun setShape(ids: Collection<Long>, shape: Int) = bulk(ids, null) { it.copy(shape = shape) }

    fun toggleLabel(ids: Collection<Long>, label: String) {
        val addAll = snapshot(ids).any { label !in it.labels }
        store.update(ids) { if (addAll) it.copy(labels = (it.labels + label).distinct()) else it.copy(labels = it.labels - label) }
    }

    fun duplicate(id: Long): Long? {
        val n = store.get(id) ?: return null
        val copyId = store.newId()
        store.upsert(n.copy(id = copyId, title = if (n.title.isBlank()) "" else n.title + " (копия)", pinned = false, reminderAt = null,
            created = System.currentTimeMillis(), edited = System.currentTimeMillis()))
        undo.value = UndoEvent("Создана копия", emptyList())
        return copyId
    }

    fun undoLast() {
        val e = undo.value ?: return
        e.snapshot.forEach { store.upsert(it); Reminders.schedule(getApplication(), it) }
        undo.value = null
    }

    fun randomNote(): Long? = sections.value.all.randomOrNull()?.id?.also { open(it) }

    // ---------------- selection ----------------
    fun toggleSelect(id: Long) {
        selection.value = selection.value.let { if (id in it) it - id else it + id }
    }

    fun selectAll() { selection.value = sections.value.all.map { it.id }.toSet() }
    fun clearSelection() { selection.value = emptySet() }

    // ---------------- labels ----------------
    fun addLabel(name: String) = store.addLabel(name)
    fun renameLabel(old: String, new: String) {
        store.renameLabel(old, new)
        if ((filter.value as? Filter.Label)?.name == old) filter.value = Filter.Label(new.trim())
    }
    fun deleteLabel(name: String) {
        store.deleteLabel(name)
        if ((filter.value as? Filter.Label)?.name == name) filter.value = Filter.All
    }

    // ---------------- settings & backup ----------------
    fun updateSettings(block: (Settings) -> Settings) = prefs.update(block)

    fun exportJson(): String = store.exportJson()

    fun importJson(json: String): Result<Int> = runCatching { store.importJson(json) }
}
