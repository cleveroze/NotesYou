package io.github.cleveroze.notesyou.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** "wallpaper" = Material You dynamic color, otherwise a seed hex */
    val colorSource: String = "#8B5E3C",
    val grid: Boolean = true,
    val columns: Int = 2,
    val sort: SortMode = SortMode.EDITED,
    val previewLines: Int = 8,
    val editorTextSize: Int = 18,
    val defaultColor: Int = 0,
    val animations: Boolean = true,
    val haptics: Boolean = true,
    val swipeToArchive: Boolean = true,
    val showDates: Boolean = true,
    val checkedToBottom: Boolean = true,
)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("notesyou", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _settings

    private fun read() = Settings(
        themeMode = runCatching { ThemeMode.valueOf(sp.getString("themeMode", "")!!) }.getOrDefault(ThemeMode.SYSTEM),
        colorSource = sp.getString("colorSource", "#8B5E3C") ?: "#8B5E3C",
        grid = sp.getBoolean("grid", true),
        columns = sp.getInt("columns", 2),
        sort = runCatching { SortMode.valueOf(sp.getString("sort", "")!!) }.getOrDefault(SortMode.EDITED),
        previewLines = sp.getInt("previewLines", 8),
        editorTextSize = sp.getInt("editorTextSize", 18),
        defaultColor = sp.getInt("defaultColor", 0),
        animations = sp.getBoolean("animations", true),
        haptics = sp.getBoolean("haptics", true),
        swipeToArchive = sp.getBoolean("swipeToArchive", true),
        showDates = sp.getBoolean("showDates", true),
        checkedToBottom = sp.getBoolean("checkedToBottom", true),
    )

    fun update(block: (Settings) -> Settings) {
        val s = block(_settings.value)
        _settings.value = s
        sp.edit()
            .putString("themeMode", s.themeMode.name)
            .putString("colorSource", s.colorSource)
            .putBoolean("grid", s.grid)
            .putInt("columns", s.columns)
            .putString("sort", s.sort.name)
            .putInt("previewLines", s.previewLines)
            .putInt("editorTextSize", s.editorTextSize)
            .putInt("defaultColor", s.defaultColor)
            .putBoolean("animations", s.animations)
            .putBoolean("haptics", s.haptics)
            .putBoolean("swipeToArchive", s.swipeToArchive)
            .putBoolean("showDates", s.showDates)
            .putBoolean("checkedToBottom", s.checkedToBottom)
            .apply()
    }
}
