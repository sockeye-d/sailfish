package dev.fishies.ranim2.gui

import androidx.compose.runtime.Stable
import dev.fishies.ranim2.Animation
import dev.fishies.ranim2.gui.util.watchFile
import dev.fishies.ranim2.ksp.AnimationMetadata
import dev.fishies.ranim2.ksp.AnimationSymbol
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.inputStream
import kotlin.time.Duration.Companion.milliseconds

@Stable
data class AnimationData(
    val name: String,
    val fn: () -> Animation?,
)

class MainScreenViewModel(private val scope: CoroutineScope, val metadataPath: Path) {
    private var loader: URLClassLoader? = null
        set(value) {
            field?.close()
            field = value
        }

    val animations = flow {
        emit(Json.decodeFromStream(metadataPath.inputStream()))
        watchFile(FileSystems.getDefault().newWatchService(), metadataPath.parent)
    }.flowOn(Dispatchers.IO).transformLatest { metadata ->
        emit(Outcome.Progress)
        val url = arrayOf(Path.of(metadata.jarFileOutputPath).absolute().toUri().toURL())
        while (true) {
            delay(100.milliseconds)
            loadJarFrom(url, metadata)
            break
        }
    }.flowOn(Dispatchers.IO)

    private val _activeAnimation = MutableStateFlow<Animation?>(null)
    val activeAnimation: StateFlow<Animation?> = _activeAnimation

    fun setActiveAnimation(animation: Animation?) {
        _activeAnimation.value = animation
        animation?.tick()
    }

    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused

    fun setPaused(paused: Boolean) {
        _paused.value = paused
    }

    private fun makeAnimationData(
        symbol: AnimationSymbol, loader: URLClassLoader
    ): AnimationData = AnimationData(symbol.fnName) {
        try {
            Thread.currentThread().contextClassLoader = loader
            loader.loadClass(symbol.ownerClassName).getMethod(symbol.fnName)(null) as Animation
        } catch (e: ClassCastException) {
            println("Animation $symbol was not of type Animation")
            println("Stack trace: ${e.stackTraceToString()}")
            null
        } catch (e: Exception) {
            println("An exception occurred when attempting to load animation $symbol: $e")
            println("Stack trace: ${e.stackTraceToString()}")
            null
        }
    }

    private suspend fun FlowCollector<Outcome<List<AnimationData>>>.loadJarFrom(
        url: Array<URL>, metadata: AnimationMetadata
    ) {
        loader = URLClassLoader.newInstance(url).also { loader ->
            try {
                val animations = metadata.animations.map { makeAnimationData(it, loader) }
                emit(Outcome.Success(animations))
                return
            } catch (e: ReflectiveOperationException) {
                println("Retrying due to $e")
            }
        }
    }
}
