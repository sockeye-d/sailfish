package dev.fishies.ranim2.gui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.fishies.ranim2.gui.util.*
import dev.fishies.ranim2.theming.LocalTheme
import dev.fishies.ranim2.theming.Theme
import dev.fishies.ranim2.util.loadJson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
import java.nio.file.Path
import kotlin.io.path.absolute

@OptIn(ExperimentalSerializationApi::class, ExperimentalCoroutinesApi::class)
fun main(args: Array<String>) = application {
    Window(onCloseRequest = ::exitApplication, title = "Animation viewer") {
        val scope = rememberCoroutineScope()
        val vm = remember {
            MainScreenViewModel(
                scope,
                args.firstOrNull()?.let { Path.of(it).absolute() },
            )
        }

        val animations by vm.animations.collectAsState(Outcome.Progress)
        val theme = loadJson<Theme>("catppuccin-mocha.json")
        val paused = vm.paused.collectAsState().value
        val activeAnimation = vm.activeAnimation.collectAsState().value
        val activeAnimationData = vm.activeAnimationData.collectAsState().value
        val stateObject = vm.cursorFrame.collectAsState()
        val cursorFrame = remember { mutableStateFrom(stateObject, vm::setCursorFrame) }
        val markers = vm.guiMarkerStorage.markers

        LaunchedEffect(Unit) {
            vm.ready()
        }

        MaterialTheme(colors = theme.toComposeColors()) {
            CompositionLocalProvider(
                LocalGraphicsContext provides rememberSkiaGraphicsContext(),
                LocalTheme provides theme,
            ) {
                MainScreen(
                    animations,
                    paused,
                    vm::setPaused,
                    activeAnimation,
                    vm::setActiveAnimation,
                    activeAnimationData,
                    cursorFrame,
                    markers,
                    vm::modifyMarker,
                )
            }
        }
    }
}
