package io.github.cleveroze.notesyou

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cleveroze.notesyou.data.Note
import io.github.cleveroze.notesyou.reminders.Reminders
import io.github.cleveroze.notesyou.ui.NotesApp
import io.github.cleveroze.notesyou.ui.NotesViewModel
import io.github.cleveroze.notesyou.ui.theme.NotesTheme

class MainActivity : ComponentActivity() {
    private val vm: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Reminders.ensureChannel(this)
        if (savedInstanceState == null) handle(intent)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            NotesTheme(settings) { NotesApp(vm) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent ?: return
        when {
            // shared text from another app → new note
            intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
                val id = vm.create()
                vm.note(id)?.let { vm.save(it.copy(title = subject, text = text)) }
            }
            // launcher shortcuts
            intent.action == "io.github.cleveroze.notesyou.NEW_NOTE" -> vm.create()
            intent.action == "io.github.cleveroze.notesyou.NEW_LIST" -> vm.create(checklist = true)
            // tapped a reminder notification
            intent.hasExtra(Reminders.EXTRA_NOTE_ID) -> {
                val id = intent.getLongExtra(Reminders.EXTRA_NOTE_ID, 0)
                vm.note(id)?.let { n: Note ->
                    vm.save(n.copy(reminderAt = null), touch = false)
                    vm.open(id)
                }
            }
        }
    }
}
