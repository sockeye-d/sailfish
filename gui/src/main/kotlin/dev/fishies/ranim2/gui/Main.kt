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
import dev.fishies.ranim2.theming.LocalTheme
import dev.fishies.ranim2.theming.Theme
import dev.fishies.ranim2.util.loadBytes
import dev.fishies.ranim2.util.loadJson
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
    val fn: () -> Animation?,
)

var loader: URLClassLoader? = null

@OptIn(ExperimentalSerializationApi::class, ExperimentalCoroutinesApi::class)
fun main(args: Array<String>) = application {
    val metadataPath = remember {
        Path.of(args.firstOrNull() ?: error("Path to metadata file must be provided")).absolute()
    }

    val animations by remember {
        flow {
            emit(Json.decodeFromStream(metadataPath.inputStream()))
            watchFilesystem(FileSystems.getDefault().newWatchService(), metadataPath.parent)
        }.flowOn(Dispatchers.IO).transformLatest { metadata ->
            emit(Outcome.Progress)
            val url = Path.of(metadata.jarFileOutputPath).absolute().toUri().toURL()
            while (true) {
                delay(500.milliseconds)
                loader?.close()
                loader = URLClassLoader.newInstance(arrayOf(url))
                val loader = loader!!
                try {
                    emit(Outcome.Success(metadata.animations.map {
                        AnimationData(it.fnName) {
                            val oldLoader = Thread.currentThread().contextClassLoader
                            try {
                                val method = loader.loadClass(it.ownerClassName).getMethod(it.fnName)
                                Thread.currentThread().contextClassLoader = loader
                                val anim = method(null) as Animation
                                // loader.close()
                                anim
                            } catch (e: Exception) {
                                println("An exception occurred when attempting to load animation ${it.ownerClassName}.${it.fnName}: $e")
                                println("Stack trace: ${e.stackTraceToString()}")
                                null
                            } finally {
                                Thread.currentThread().contextClassLoader = oldLoader
                            }
                        }
                    }))
                    break
                } catch (e: ReflectiveOperationException) {
                    println("Retrying due to $e")
                }
            }
        }
            .flowOn(Dispatchers.IO)
    }.collectAsState(Outcome.Progress)

    val theme = loadJson<Theme>("catppuccin-mocha.json", ::loadBytes::class.java.classLoader)

    @OptIn(InternalComposeUiApi::class) CompositionLocalProvider(
        LocalGraphicsContext provides remember { SkiaGraphicsContext() },
        LocalTheme provides theme,
    ) {
        MaterialTheme(colors = LocalTheme.current.toComposeColors()) {
            Window(onCloseRequest = ::exitApplication, title = "My Desktop App") {
                // val animations by remember {
                //     flow {
                //         while (true) {
                //             emit(Outcome.Progress)
                //             delay(1.seconds)
                //             emit(Outcome.Success(emptyList<AnimationData>()))
                //             delay(1.seconds)
                //         }
                //     }
                // }.collectAsState(Outcome.Progress)
                var paused by remember { mutableStateOf(true) }
                var activeAnimation: Animation? by remember { mutableStateOf(null) }
                MainScreen(animations, paused, { paused = it }, activeAnimation) {
                    activeAnimation = it
                    activeAnimation?.tick()
                }
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
            delay(1.milliseconds)
        }

        for (event in key.pollEvents()) {
            val kind = event.kind()
            if (kind != StandardWatchEventKinds.ENTRY_MODIFY) {
                continue
            }
            val path = event.context() as? Path ?: continue
            emit(Json.decodeFromStream(path.inputStream()))
        }

        key.reset()
    }
}
