package dev.fishies.ranim2.gui

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.SkiaGraphicsContext
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.fishies.ranim2.Animation
import dev.fishies.ranim2.ksp.AnimationMetadata
import dev.fishies.ranim2.theming.Theme
import dev.fishies.ranim2.theming.defaultTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.net.URLClassLoader
import java.nio.file.*
import kotlin.io.path.absolute
import kotlin.io.path.inputStream
import kotlin.time.Duration.Companion.milliseconds

@Stable
data class AnimationData(
    val name: String,
    val fn: () -> Animation,
)

@OptIn(ExperimentalSerializationApi::class)
fun main(args: Array<String>) = application {
    val metadataPath = remember {
        Path.of(args.firstOrNull() ?: error("Path to metadata file must be provided")).absolute()
    }

    val animations by remember {
        flow {
            emit(Json.decodeFromStream(metadataPath.inputStream()))
            val watcher = FileSystems.getDefault().newWatchService()
            watchFilesystem(watcher, metadataPath.parent)
        }.flowOn(Dispatchers.IO).map { metadata ->
            val path = Path.of(metadata.jarFileOutputPath).absolute().toUri().toURL()
            println(path)
            val loader = URLClassLoader.newInstance(arrayOf(path))
            Thread.currentThread().contextClassLoader = loader
            metadata.animations.map {
                val method = loader.loadClass(it.ownerClassName).getMethod(it.fnName)
                AnimationData(it.fnName) {
                    method.invoke(null) as Animation
                }
            }
        }
    }.collectAsState(emptyList())

    @OptIn(InternalComposeUiApi::class) CompositionLocalProvider(LocalGraphicsContext provides remember { SkiaGraphicsContext() }) {
        MaterialTheme(colors = defaultTheme.toComposeColors()) {
            Window(onCloseRequest = ::exitApplication, title = "My Desktop App") {
                var paused by remember { mutableStateOf(true) }
                var activeAnimation: Animation? by remember { mutableStateOf(null) }
                MainScreen(animations, paused, { paused = it }, activeAnimation, { activeAnimation = it })
            }
        }
    }
}

fun Theme.toComposeColors() = Colors(
    primary = primary,
    primaryVariant = primaryVariant,
    secondary = secondary,
    secondaryVariant = secondaryVariant,
    background = background,
    surface = surface,
    error = error,
    onPrimary = onPrimary,
    onSecondary = onSecondary,
    onBackground = onBackground,
    onSurface = onSurface,
    onError = onError,
    isLight = isLight
)

@OptIn(ExperimentalSerializationApi::class)
suspend fun FlowCollector<AnimationMetadata>.watchFilesystem(watcher: WatchService, metadataPath: Path) {
    withContext(Dispatchers.IO) {
        metadataPath.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY)
    }

    while (true) {
        var key: WatchKey? = null
        while (key == null) {
            key = watcher.poll()
            delay(500.milliseconds)
        }

        for (event in key.pollEvents()) {
            val kind = event.kind()
            if (kind != StandardWatchEventKinds.ENTRY_MODIFY) {
                continue
            }
            val path = event.context() as? Path ?: continue
            emit(Json.decodeFromStream(path.inputStream()))
        }
    }
}
