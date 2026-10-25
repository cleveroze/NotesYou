package io.github.cleveroze.notesyou.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Notes and labels persisted as a single JSON file (atomic writes, debounced). */
class NotesStore(context: Context) {
    private val file = File(context.filesDir, "notes.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    private val _labels = MutableStateFlow<List<String>>(emptyList())
    val labels: StateFlow<List<String>> = _labels

    init {
        load()
    }

    private fun load() {
        val o = runCatching { JSONObject(file.readText()) }.getOrNull()
        if (o == null) {
            _notes.value = Samples.notes()
            _labels.value = Samples.labels
            save()
            return
        }
        val arr = o.optJSONArray("notes") ?: JSONArray()
        val month = 30L * 24 * 3600 * 1000
        val now = System.currentTimeMillis()
        _notes.value = (0 until arr.length()).mapNotNull { runCatching { Note.fromJson(arr.getJSONObject(it)) }.getOrNull() }
            .filter { it.trashedAt == null || now - it.trashedAt < month } // trash empties itself after 30 days
        val l = o.optJSONArray("labels") ?: JSONArray()
        _labels.value = (0 until l.length()).map { l.getString(it) }
    }

    private fun save() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(250)
            writeNow()
        }
    }

    private fun writeNow() {
        val o = JSONObject()
            .put("version", 1)
            .put("notes", JSONArray(_notes.value.map { it.toJson() }))
            .put("labels", JSONArray(_labels.value))
        val tmp = File(file.parentFile, "notes.json.tmp")
        tmp.writeText(o.toString())
        tmp.renameTo(file)
    }

    fun get(id: Long): Note? = _notes.value.firstOrNull { it.id == id }

    fun upsert(note: Note) {
        val list = _notes.value
        _notes.value = if (list.any { it.id == note.id }) list.map { if (it.id == note.id) note else it } else listOf(note) + list
        note.labels.filterNot { it in _labels.value }.takeIf { it.isNotEmpty() }?.let { _labels.value = _labels.value + it }
        save()
    }

    fun update(ids: Collection<Long>, edit: (Note) -> Note) {
        _notes.value = _notes.value.map { if (it.id in ids) edit(it) else it }
        save()
    }

    fun deleteForever(ids: Collection<Long>) {
        _notes.value = _notes.value.filterNot { it.id in ids }
        save()
    }

    fun emptyTrash() {
        _notes.value = _notes.value.filterNot { it.isTrashed }
        save()
    }

    fun addLabel(name: String) {
        val n = name.trim()
        if (n.isNotEmpty() && n !in _labels.value) { _labels.value = _labels.value + n; save() }
    }

    fun renameLabel(old: String, new: String) {
        val n = new.trim()
        if (n.isEmpty()) return
        _labels.value = _labels.value.map { if (it == old) n else it }.distinct()
        _notes.value = _notes.value.map { note -> if (old in note.labels) note.copy(labels = note.labels.map { if (it == old) n else it }.distinct()) else note }
        save()
    }

    fun deleteLabel(name: String) {
        _labels.value = _labels.value - name
        _notes.value = _notes.value.map { if (name in it.labels) it.copy(labels = it.labels - name) else it }
        save()
    }

    // ---- export / import ----
    fun exportJson(): String = JSONObject()
        .put("app", "NotesYou").put("version", 1)
        .put("notes", JSONArray(_notes.value.map { it.toJson() }))
        .put("labels", JSONArray(_labels.value))
        .toString(2)

    /** Returns how many notes were imported. */
    fun importJson(json: String): Int {
        val o = JSONObject(json)
        val arr = o.getJSONArray("notes")
        val existing = _notes.value.map { it.id }.toSet()
        var nextId = (_notes.value.maxOfOrNull { it.id } ?: 0L) + 1
        val imported = (0 until arr.length()).map {
            val n = Note.fromJson(arr.getJSONObject(it))
            if (n.id in existing) n.copy(id = nextId++) else n
        }
        _notes.value = imported + _notes.value
        val l = o.optJSONArray("labels") ?: JSONArray()
        _labels.value = (_labels.value + (0 until l.length()).map { l.getString(it) } + imported.flatMap { it.labels }).distinct()
        save()
        return imported.size
    }

    fun newId(): Long = maxOf(System.currentTimeMillis(), (_notes.value.maxOfOrNull { it.id } ?: 0L) + 1)
}

/** A few welcome notes so the first launch isn't empty. */
object Samples {
    val labels = listOf("Идеи", "Личное", "Работа")

    fun notes(): List<Note> {
        val now = System.currentTimeMillis()
        return listOf(
            Note(
                id = now, title = "Добро пожаловать в Заметки ✨",
                text = "Зажмите заметку, чтобы выбрать несколько.\nСмахните влево или вправо — в архив.\n" +
                    "Каждой заметке можно дать цвет и фигуру Material: печеньку, клевер или солнце.",
                color = 3, shape = 0, pinned = true, labels = listOf("Идеи"), created = now, edited = now,
            ),
            Note(
                id = now - 1, title = "Список покупок", isChecklist = true, color = 5, shape = 1,
                items = listOf(
                    CheckItem(1, "Молоко", true), CheckItem(2, "Хлеб"), CheckItem(3, "Сыр"), CheckItem(4, "Яблоки"),
                ),
                labels = listOf("Личное"), created = now - 60_000, edited = now - 60_000,
            ),
            Note(
                id = now - 2, title = "Идея", text = "Сделать свою ОС — RippleOS 🌊", color = 7, shape = 2,
                labels = listOf("Идеи"), created = now - 120_000, edited = now - 120_000,
            ),
        )
    }
}
