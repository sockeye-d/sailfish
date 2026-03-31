package dev.fishies.sailfish.gui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.withFrameMillis
import dev.fishies.sailfish.*
import dev.fishies.sailfish.elements.text
import dev.fishies.sailfish.gui.util.*
import dev.fishies.sailfish.ksp.AnimationMetadata
import dev.fishies.sailfish.ksp.AnimationSymbol
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import org.gradle.tooling.BuildException
import org.gradle.tooling.GradleConnector
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.time.Duration.Companion.milliseconds

@Immutable
data class AnimationData(
    val name: String,
    val symbol: AnimationSymbol,
    val factory: () -> Animation?,
) : () -> Animation? by factory

@Immutable
data class CurrentAnimationState(
    val animation: Animation,
    val data: AnimationData,
    val animationLength: Int,
    val markers: Map<String, Marker>,
    val paused: Boolean,
)

class MainScreenViewModel(private val scope: CoroutineScope, val metadataPath: Path?) {
    private lateinit var loader: URLClassLoader

    val json = Json {
        prettyPrint = true
        isLenient = true
    }

    val defaultAnimations = listOf(
        AnimationData("Test", AnimationSymbol("", "Test", "", AnimationSymbol.Data()), ::testAnimation)
    )

    val guiMarkerStorage = GuiMarkerStorage()

    private val activeAnimationData = MutableStateFlow<AnimationData?>(null)
    private val activeAnimation = MutableStateFlow<Animation?>(null)
    private val animationLength = MutableStateFlow(0)
    private val pausedFlow = MutableStateFlow(false)
    private var paused by pausedFlow

    val animationState = combine(
        activeAnimationData,
        activeAnimation,
        animationLength,
        guiMarkerStorage.markers,
        pausedFlow,
    ) { animationData, animation, length, markers, paused ->
        if (animationData == null || animation == null) {
            null
        } else {
            CurrentAnimationState(
                animation,
                animationData,
                length,
                markers,
                paused,
            )
        }
    }

    val projectDir = metadataPath?.parent
    val connection = projectDir?.let {
        GradleConnector.newConnector().forProjectDirectory(it.toFile()).connect()
    }

    val animationMetadata = if (projectDir != null && connection != null && metadataPath != null) {
        flow {
            emit(null)
            watchDir(
                path = projectDir.resolve("src/"), projectDir.resolve("src/main/resources/markers.json")
            ) { it, _ ->
                if (!it.endsWith("~")) {
                    emit(it)
                }
            }
        }.flowOn(Dispatchers.IO).debounce(100.milliseconds).transformLatest {
            if (it != null) {
                emit(Outcome.Progress)
                println("Rebuilding due to $it")
                try {
                    runInterruptible {
                        connection.newBuild().forTasks("jar").setStandardOutput(System.out).run()
                    }
                } catch (e: BuildException) {
                    println("Build failed :(")
                    emit(Outcome.Error(e))
                    return@transformLatest
                }
                println("Rebuilt!")
            }
            emit(Outcome.Success(json.decodeFromString<AnimationMetadata>(metadataPath.readText())))
        }.flowOn(Dispatchers.IO)
    } else {
        emptyFlow()
    }.stateIn(scope, SharingStarted.Eagerly, null)

    @OptIn(FlowPreview::class)
    val animations = if (metadataPath != null) {
        animationMetadata.filterNotNull().transformLatest { metadata ->
            emit(Outcome.Progress)
            if (metadata is Outcome.Success) {
                val (data) = metadata
                val jarUrl: URL = Path.of(data.jarFileOutputPath).absolute().toUri().toURL()
                loadJarFrom(arrayOf(jarUrl), data) {
                    emit(Outcome.Success(it))
                }
            }
        }.flowOn(Dispatchers.IO)
    } else {
        flowOf(Outcome.Success(defaultAnimations))
    }

    fun ready() {
        scope.launch {
            var lastTick: Long? = null
            while (true) {
                withFrameMillis { tick ->
                    if (lastTick == null) {
                        lastTick = tick
                    }
                    val framerate = (activeAnimationData.value ?: return@withFrameMillis).symbol.data.framerate
                    if (tick - lastTick!! > 1000 / framerate) {
                        lastTick = tick
                        tickFrame()
                    }
                }
            }
        }
        scope.launch {
            animations.collect { animations ->
                val currentAnimationData = activeAnimationData.value
                if (animations is Outcome.Success && currentAnimationData != null) {
                    val x = animations.data
                    setActiveAnimation(x.find { it.name == currentAnimationData.name })
                }
            }
        }
        scope.launch {
            combine(guiMarkerStorage.markers, animationMetadata.filterNotNull(), activeAnimationData) { a, b, c ->
                Triple(a, b, c)
            }.collect { (markers, meta, data) ->
                if (data != null && meta is Outcome.Success) {
                    val key = data.symbol.simpleString()
                    val markers = markers.mapValues { (_, value) -> value.position }
                    val markerStoragePath = Path.of(meta.data.resourceDir).resolve("markers.json")
                    markerStoragePath.createParentDirectories()
                    val data = if (markerStoragePath.exists()) {
                        runCatching { json.decodeFromString<Map<String, Map<String, Frames>>>(markerStoragePath.readText()) }.getOrElse { emptyMap() }
                    } else {
                        emptyMap()
                    }.toMutableMap()
                    data[key] = (data[key] ?: emptyMap()) + markers
                    markerStoragePath.writeText(json.encodeToString(data))
                }
            }
        }
    }

    fun setActiveAnimation(animation: AnimationData?) {
        activeAnimationData.value = animation
        val loadedMarkers = animation?.loadMarkers() ?: emptyMap()
        val markers = mutableMapOf<String, Marker>()
        val tempMarkerStorage = object : MarkerStorage {
            override fun get(name: String) = markers.getOrPut(name) { Marker(loadedMarkers[name]?.position ?: 0, name) }
        }
        Markers.storage = tempMarkerStorage
        calculateAnimationStats(animation)
        guiMarkerStorage.setMarkers(markers)
        Markers.storage = guiMarkerStorage
        activeAnimation.value = animation?.invoke()
        if (animation != null) {
            setCursorFrame(cursorFrame.value, force = true)
        }
    }

    private fun AnimationData.loadMarkers(): Map<String, Marker> {
        val meta = animationMetadata.value?.dataOrNull ?: return emptyMap()
        val markerStoragePath = Path.of(meta.resourceDir).resolve("markers.json")
        return try {
            Json.decodeFromString<Map<String, Map<String, Frames>>>(markerStoragePath.readText())[symbol.simpleString()]!!
        } catch (_: Exception) {
            return emptyMap()
        }.mapValues { (key, value) -> Marker(value, key) }
    }

    fun calculateAnimationStats(animation: AnimationData? = activeAnimationData.value) {
        val freshAnimation = animation?.factory() ?: return
        var _animationLength = -1
        while (!freshAnimation.isFinished) {
            freshAnimation.tick()
            _animationLength++
        }
        animationLength.value = _animationLength
    }

    fun restartAnimation(): Animation? {
        val freshAnimation = (activeAnimationData.value ?: return null)()
        activeAnimation.value = freshAnimation
        return freshAnimation
    }

    @JvmName("setPausedSetter")
    fun setPaused(paused: Boolean) {
        this.paused = paused
    }

    val cursorFrame: StateFlow<Int>
        field = MutableStateFlow(1)

    fun setCursorFrame(cursor: Int, force: Boolean = false) {
        val cursor = cursor.coerceIn(1..animationLength.value.coerceAtLeast(1))
        if (cursor == cursorFrame.value && !force) return
        seekAnimationTo(cursor)
        cursorFrame.value = cursor
        paused = true
    }

    private fun seekAnimationTo(cursor: Int) {
        if (cursor <= cursorFrame.value) {
            val animation = restartAnimation() ?: return
            for (i in 1..cursor) {
                if (animation.isFinished) {
                    break
                }
                animation.tick()
            }
        } else {
            val activeAnimation = activeAnimation.value ?: return
            for (i in 1..cursor - activeAnimation.ticks) {
                if (activeAnimation.isFinished) {
                    break
                }
                activeAnimation.tick()
            }
        }
    }

    fun seekBy(delta: Int) {
        seekAnimationTo(cursorFrame.value + delta)
        cursorFrame.value += delta
    }

    fun tickFrame() {
        if (paused) return
        val activeAnimation = activeAnimation.value ?: return
        if (!activeAnimation.isFinished) {
            seekBy(1)
        }
    }

    fun seekToEnd() {
        seekAnimationTo(animationLength.value)
    }

    fun seekToStart() {
        seekAnimationTo(1)
    }

    private fun makeAnimationData(
        symbol: AnimationSymbol, loader: URLClassLoader
    ): AnimationData {
        val clazz = loader.loadClass(symbol.ownerClassName)!!
        return AnimationData(symbol.fnName, symbol) {
            try {
                Thread.currentThread().contextClassLoader = loader
                clazz.getMethod(symbol.fnName)(null) as Animation
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
    }

    private suspend fun loadJarFrom(
        url: Array<URL>,
        metadata: AnimationMetadata,
        onSuccess: suspend (List<AnimationData>) -> Unit,
    ) {
        while (true) {
            if (::loader.isInitialized) loader.close()
            loader = URLClassLoader(url)
            try {
                onSuccess(metadata.animations.map { makeAnimationData(it, loader) })
                return
            } catch (e: ReflectiveOperationException) {
                println("Retrying due to $e")
            }
            delay(100.milliseconds)
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

    fun setMarker(name: String, newValue: Marker) {
        guiMarkerStorage.addMarker(name, newValue)
        calculateAnimationStats()
        seekBy(0)
    }

    class GuiMarkerStorage internal constructor() : MarkerStorage {
        val markers: StateFlow<Map<String, Marker>>
            field = MutableStateFlow(emptyMap())

        override fun get(name: String) = markers.value[name] ?: Marker(0, name).also { addMarker(name, it) }

        fun setMarkers(markers: Map<String, Marker>) {
            this.markers.value = markers
        }

        fun addMarker(name: String, newValue: Marker) {
            markers.value += name to newValue
        }

        fun clearMarkers() {
            markers.value = emptyMap()
        }
    }
}
