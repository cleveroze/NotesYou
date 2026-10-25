@file:OptIn(ExperimentalSharedTransitionApi::class)

package io.github.cleveroze.notesyou.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun NotesApp(vm: NotesViewModel) {
    val openId by vm.openNoteId.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val motion = MaterialTheme.motionScheme
    CompositionLocalProvider(LocalAnimations provides settings.animations, LocalHaptics provides settings.haptics) {
        SharedTransitionLayout(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            AnimatedContent(
                targetState = openId,
                transitionSpec = {
                    if (!settings.animations) fadeIn(motion.fastEffectsSpec()).togetherWith(fadeOut(motion.fastEffectsSpec()))
                    else (fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), initialScale = 0.96f))
                        .togetherWith(fadeOut(motion.fastEffectsSpec()) + scaleOut(targetScale = 0.98f))
                },
                label = "nav",
            ) { id ->
                CompositionLocalProvider(LocalSharedScope provides this@SharedTransitionLayout, LocalNavScope provides this) {
                    if (id == null) HomeScreen(vm) else EditorScreen(vm, id)
                }
            }
        }
    }
}
