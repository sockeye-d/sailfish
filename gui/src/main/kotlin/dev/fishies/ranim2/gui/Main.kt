package dev.fishies.ranim2.gui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.fishies.ranim2.gui.util.rememberSkiaGraphicsContext
import dev.fishies.ranim2.gui.util.toComposeColors
import dev.fishies.ranim2.theming.LocalTheme
import dev.fishies.ranim2.theming.Theme
import dev.fishies.ranim2.util.loadJson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import java.nio.file.Path
import kotlin.io.path.absolute

@OptIn(ExperimentalSerializationApi::class, ExperimentalCoroutinesApi::class)
fun main(args: Array<String>) = application {
    val scope = rememberCoroutineScope()
    val vm = remember {
        MainScreenViewModel(
            scope, Path.of(args.firstOrNull() ?: error("Path to metadata file must be provided")).absolute()
        )
    }

    val animations by vm.animations.collectAsState(Outcome.Progress)
    val theme = loadJson<Theme>("catppuccin-mocha.json")
    val paused = vm.paused.collectAsState().value
    val activeAnimation = vm.activeAnimation.collectAsState().value

    CompositionLocalProvider(
        LocalGraphicsContext provides rememberSkiaGraphicsContext(),
        LocalTheme provides theme,
    ) {
        MaterialTheme(colors = LocalTheme.current.toComposeColors()) {
            Window(onCloseRequest = ::exitApplication, title = "Loading from ${vm.metadataPath}") {
                MainScreen(animations, paused, vm::setPaused, activeAnimation, vm::setActiveAnimation)
            }
        }
    }
}
