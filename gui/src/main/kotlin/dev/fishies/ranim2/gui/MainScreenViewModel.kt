package dev.fishies.ranim2.gui

import androidx.compose.runtime.Stable
import dev.fishies.ranim2.Animation
import dev.fishies.ranim2.animation
import dev.fishies.ranim2.elements.text
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
    val factory: () -> Animation?,
) : () -> Animation? by factory

class MainScreenViewModel(private val scope: CoroutineScope, val metadataPath: Path?) {
    private var loader: URLClassLoader? = null
        set(value) {
            field?.close()
            field = value
        }

    val animations = if (metadataPath != null) {
        flow {
            emit(Json.decodeFromStream(metadataPath.inputStream()))
            watchFile(FileSystems.getDefault().newWatchService(), metadataPath.parent)
        }.flowOn(Dispatchers.IO).transformLatest { metadata ->
            emit(Outcome.Progress)
            val jarUrl: URL = Path.of(metadata.jarFileOutputPath).absolute().toUri().toURL()
            retry@ while (true) {
                delay(100.milliseconds)
                loadJarFrom(arrayOf(jarUrl), metadata) { break@retry }
            }
        }.flowOn(Dispatchers.IO)
    } else {
        flowOf(Outcome.Success(listOf(AnimationData("Test") { testAnimation() })))
    }

    private var currentAnimationData: AnimationData? = null
    private val _activeAnimation = MutableStateFlow<Animation?>(null)
    val activeAnimation: StateFlow<Animation?> = _activeAnimation

    fun setActiveAnimation(animation: AnimationData) {
        currentAnimationData = animation
        val freshAnimation = animation()
        _activeAnimation.value = freshAnimation
        freshAnimation?.tick()
    }

    fun restartAnimation(): Animation? {
        val freshAnimation = (currentAnimationData ?: return null)()
        _activeAnimation.value = freshAnimation
        return freshAnimation
    }

    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused

    fun setPaused(paused: Boolean) {
        _paused.value = paused
    }

    private val _cursorFrame = MutableStateFlow(0)
    val cursorFrame: StateFlow<Int> = _cursorFrame

    fun setCursorFrame(cursor: Int) {
        val cursor = cursor.coerceAtLeast(1)
        if (cursor == _cursorFrame.value) return
        seekAnimationTo(cursor)
        _cursorFrame.value = cursor
    }

    private fun seekAnimationTo(cursor: Int) {
        if (cursor < _cursorFrame.value) {
            val animation = restartAnimation() ?: return
            repeat(cursor) {
                animation.tick()
            }
        } else {
            val activeAnimation = _activeAnimation.value ?: return
            repeat(cursor - activeAnimation.ticks) {
                activeAnimation.tick()
            }
        }
    }

    fun seekBy(delta: Int) {
        seekAnimationTo(_cursorFrame.value + delta)
        _cursorFrame.value += delta
    }

    fun tickFrame() {
        if (paused.value) return
        val activeAnimation = activeAnimation.value ?: return
        if (!activeAnimation.isFinished) {
            seekBy(1)
        }
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

    private suspend inline fun FlowCollector<Outcome<List<AnimationData>>>.loadJarFrom(
        url: Array<URL>, metadata: AnimationMetadata, onSuccess: () -> Nothing,
    ) {
        loader = URLClassLoader.newInstance(url).also { loader ->
            try {
                val animations = metadata.animations.map { makeAnimationData(it, loader) }
                emit(Outcome.Success(animations))
                onSuccess()
            } catch (e: ReflectiveOperationException) {
                println("Retrying due to $e")
            }
        }
    }

    private fun testAnimation() = animation {
        val t = text("0")
        while (true) {
            t.text = (t.text.toInt() + 1).toString()
            yield()
        }
    }
}
