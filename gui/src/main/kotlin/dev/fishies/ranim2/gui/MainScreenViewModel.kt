package dev.fishies.ranim2.gui

import androidx.compose.runtime.*
import dev.fishies.ranim2.*
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
    val symbol: AnimationSymbol,
    val factory: () -> Animation?,
) : () -> Animation? by factory

class MainScreenViewModel(private val scope: CoroutineScope, val metadataPath: Path?) {
    private var loader: URLClassLoader? = null
        set(value) {
            field?.apply {
                println("Closing old loader...")
                close()
            }
            field = value
        }

    fun ready() {
        scope.launch {
            var lastTick: Long? = null
            while (true) {
                withFrameMillis { tick ->
                    if (lastTick == null) {
                        lastTick = tick
                    }
                    val framerate = activeAnimationData.value?.symbol?.data?.framerate ?: return@withFrameMillis
                    if (tick - lastTick!! > 1000 / framerate) {
                        lastTick = tick
                        tickFrame()
                    }
                }
            }
        }
        scope.launch {
            animations.collect { animations ->
                val currentAnimationData = _activeAnimationData.value
                if (animations is Outcome.Success && currentAnimationData != null) {
                    val x = animations.data
                    setActiveAnimation(x.find { it.name == currentAnimationData.name })
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    val animations = if (metadataPath != null) {
        flow {
            emit(metadataPath)
            watchFile(FileSystems.getDefault().newWatchService(), metadataPath)
        }.flowOn(Dispatchers.IO).map {
            Json.decodeFromStream<AnimationMetadata>(it.inputStream())
        }.transformLatest { metadata ->
            emit(Outcome.Progress)
            flow {
                watchFile(
                    FileSystems.getDefault().newWatchService(),
                    Path.of(metadata.jarFileOutputPath)
                )
            }.debounce(500.milliseconds).first()
            emit(Outcome.Success(metadata))
        }.transformLatest { metadata ->
            emit(Outcome.Progress)
            if (metadata is Outcome.Success) {
                val jarUrl: URL = Path.of(metadata.data.jarFileOutputPath).absolute().toUri().toURL()
                retry@ while (true) {
                    loadJarFrom(arrayOf(jarUrl), metadata.data) { break@retry }
                    delay(100.milliseconds)
                }
            }
        }.flowOn(Dispatchers.IO)
    } else {
        flowOf(
            Outcome.Success(
                listOf(
                    AnimationData(
                        "Test",
                        AnimationSymbol("", "Test", "", AnimationSymbol.Data())
                    ) { testAnimation() })
            )
        )
    }

    private var _activeAnimationData = MutableStateFlow<AnimationData?>(null)
    val activeAnimationData: StateFlow<AnimationData?> = _activeAnimationData

    private val _activeAnimation = MutableStateFlow<Animation?>(null)
    val activeAnimation: StateFlow<Animation?> = _activeAnimation

    fun setActiveAnimation(animation: AnimationData?) {
        _activeAnimationData.value = animation
        if (animation == null) {
            _activeAnimation.value = null
            calculateAnimationStats(null)
        } else {
            calculateAnimationStats(animation)
            val freshAnimation = animation()
            _activeAnimation.value = freshAnimation
            setCursorFrame(cursorFrame.value, force = true)
        }
    }

    fun calculateAnimationStats(animation: AnimationData?) {
        Markers.storage = guiMarkerStorage
        guiMarkerStorage.clearMarkers()
        val freshAnimation = animation?.factory() ?: return
        while (!freshAnimation.isFinished) {
            freshAnimation.tick()
        }
    }

    fun restartAnimation(): Animation? {
        val freshAnimation = (_activeAnimationData.value ?: return null)()
        _activeAnimation.value = freshAnimation
        return freshAnimation
    }

    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused

    fun setPaused(paused: Boolean) {
        _paused.value = paused
    }

    private val _cursorFrame = MutableStateFlow(1)
    val cursorFrame: StateFlow<Int> = _cursorFrame

    fun setCursorFrame(cursor: Int, force: Boolean = false) {
        val cursor = cursor.coerceAtLeast(1)
        if (cursor == _cursorFrame.value && !force) return
        seekAnimationTo(cursor)
        _cursorFrame.value = cursor
    }

    private fun seekAnimationTo(cursor: Int) {
        if (cursor <= _cursorFrame.value) {
            val animation = restartAnimation() ?: return
            for (i in 1..cursor) {
                if (animation.isFinished) {
                    break
                }
                animation.tick()
            }
        } else {
            val activeAnimation = _activeAnimation.value ?: return
            for (i in 1..cursor - activeAnimation.ticks) {
                if (activeAnimation.isFinished) {
                    break
                }
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
    ): AnimationData = AnimationData(symbol.fnName, symbol) {
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
        yield("start")
        while (!isPast("end")) {
            t.text = (t.text.toInt() + 1).toString()
            yield()
        }
    }

    fun modifyMarker(name: String, newValue: Marker) {
        guiMarkerStorage.markers += name to newValue
        seekBy(0)
    }

    val guiMarkerStorage = GuiMarkerStorage()

    class GuiMarkerStorage : MarkerStorage {
        var markers by mutableStateOf(emptyMap<String, Marker>())
        override fun get(name: String) = markers[name] ?: Marker(0, name).also { markers += name to it }
        fun clearMarkers() {
            markers = emptyMap()
        }
    }
}
