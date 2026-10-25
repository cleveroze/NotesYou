package io.github.cleveroze.notesyou.data

import org.json.JSONArray
import org.json.JSONObject

data class CheckItem(val id: Long, val text: String, val checked: Boolean = false)

data class Note(
    val id: Long,
    val title: String = "",
    val text: String = "",
    val isChecklist: Boolean = false,
    val items: List<CheckItem> = emptyList(),
    /** index in [NoteColors]; 0 = default surface */
    val color: Int = 0,
    /** index in the expressive shape list used as the note's badge; -1 = none */
    val shape: Int = -1,
    val labels: List<String> = emptyList(),
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val trashedAt: Long? = null,
    val reminderAt: Long? = null,
    val created: Long = System.currentTimeMillis(),
    val edited: Long = System.currentTimeMillis(),
) {
    val isTrashed get() = trashedAt != null
    val isBlank get() = title.isBlank() && text.isBlank() && items.all { it.text.isBlank() }

    /** Plain text representation (for sharing, search, counting). */
    fun plain(): String = buildString {
        if (title.isNotBlank()) appendLine(title)
        if (isChecklist) items.forEach { appendLine((if (it.checked) "☑ " else "☐ ") + it.text) }
        else append(text)
    }.trim()

    fun words(): Int = plain().split(Regex("\\s+")).count { it.isNotBlank() }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("title", title).put("text", text).put("isChecklist", isChecklist)
        .put("items", JSONArray(items.map { JSONObject().put("id", it.id).put("text", it.text).put("checked", it.checked) }))
        .put("color", color).put("shape", shape).put("labels", JSONArray(labels))
        .put("pinned", pinned).put("archived", archived)
        .put("trashedAt", trashedAt ?: JSONObject.NULL).put("reminderAt", reminderAt ?: JSONObject.NULL)
        .put("created", created).put("edited", edited)

    companion object {
        fun fromJson(o: JSONObject): Note {
            val itemsArr = o.optJSONArray("items") ?: JSONArray()
            val labelsArr = o.optJSONArray("labels") ?: JSONArray()
            return Note(
                id = o.getLong("id"),
                title = o.optString("title"),
                text = o.optString("text"),
                isChecklist = o.optBoolean("isChecklist"),
                items = (0 until itemsArr.length()).map {
                    val i = itemsArr.getJSONObject(it)
                    CheckItem(i.getLong("id"), i.optString("text"), i.optBoolean("checked"))
                },
                color = o.optInt("color"),
                shape = o.optInt("shape", -1),
                labels = (0 until labelsArr.length()).map { labelsArr.getString(it) },
                pinned = o.optBoolean("pinned"),
                archived = o.optBoolean("archived"),
                trashedAt = if (o.isNull("trashedAt")) null else o.optLong("trashedAt"),
                reminderAt = if (o.isNull("reminderAt")) null else o.optLong("reminderAt"),
                created = o.optLong("created", System.currentTimeMillis()),
                edited = o.optLong("edited", System.currentTimeMillis()),
            )
        }
    }
}

enum class SortMode(val title: String) {
    EDITED("По изменению"), CREATED("По созданию"), TITLE("По названию"), COLOR("По цвету"), REMINDER("По напоминанию")
}

/** Where the user is looking: main list, reminders, archive, trash or a label. */
sealed interface Filter {
    data object All : Filter
    data object Reminders : Filter
    data object Archive : Filter
    data object Trash : Filter
    data class Label(val name: String) : Filter
}
