package dev.fishies.sailfish.gui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.withFrameMillis
import dev.fishies.sailfish.*
import dev.fishies.sailfish.elements.text
import dev.fishies.sailfish.gui.util.watchDir
import dev.fishies.sailfish.gui.util.watchFile
import dev.fishies.sailfish.ksp.AnimationMetadata
import dev.fishies.sailfish.ksp.AnimationSymbol
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
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
    private var loader: URLClassLoader? = null
        set(value) {
            field?.apply {
                println("Closing old loader...")
                close()
            }
            field = value
        }

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

    val animationMetadata = if (metadataPath != null) {
        flow {
            emit(json.decodeFromStream<AnimationMetadata>(metadataPath.inputStream()) to true)
            flow {
                watchFile(metadataPath) {
                    emit(it)
                }
            }.collect {
                emit(json.decodeFromStream<AnimationMetadata>(it.inputStream()) to false)
            }
        }.flowOn(Dispatchers.IO)
    } else {
        emptyFlow()
    }.stateIn(scope, SharingStarted.Eagerly, null)

    @OptIn(FlowPreview::class)
    val animations = if (metadataPath != null) {
        animationMetadata.filterNotNull().transformLatest { (metadata, force) ->
            if (force) {
                emit(Outcome.Success(metadata))
            } else {
                emit(Outcome.Progress)
                flow {
                    watchFile(Path.of(metadata.jarFileOutputPath)) {
                        emit(it)
                    }
                }.flowOn(Dispatchers.IO).debounce(500.milliseconds).first()
                emit(Outcome.Success(metadata))
            }
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
        flowOf(Outcome.Success(defaultAnimations))
    }

    fun ready() {
        if (metadataPath != null) {
            val projectDir = metadataPath.parent
            val connection = GradleConnector.newConnector().forProjectDirectory(projectDir.toFile()).connect()
            scope.launch(Dispatchers.IO) {
                flow {
                    watchDir(
                        path = projectDir.resolve("src/"),
                        projectDir.resolve("src/main/resources/markers.json")
                    ) { it, _ ->
                        if (!it.endsWith("~")) {
                            emit(it)
                        }
                    }
                }.debounce(10.milliseconds).collectLatest {
                    println("Rebuilding due to $it")
                    runInterruptible {
                        connection.newBuild().forTasks("jar").run()
                    }
                }
            }
        }
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
            combine(guiMarkerStorage.markers, animationMetadata.filterNotNull(), activeAnimationData) { a, (b), c ->
                Triple(a, b, c)
            }.flowOn(Dispatchers.IO).collect { (markers, meta, data) ->
                if (data != null) {
                    val markers = mapOf(
                        data.symbol.simpleString() to markers.mapValues { (_, value) -> value.position },
                    )
                    val markerStoragePath = Path.of(meta.resourceDir).resolve("markers.json")
                    markerStoragePath.createParentDirectories()
                    val data = if (markerStoragePath.exists()) {
                        runCatching { json.decodeFromString<Map<String, Map<String, Frames>>>(markerStoragePath.readText()) }.getOrElse { emptyMap() }
                    } else {
                        emptyMap()
                    } + markers
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
        val meta = animationMetadata.value ?: return emptyMap()
        val markerStoragePath = Path.of(meta.first.resourceDir).resolve("markers.json")
        return try {
            Json.decodeFromString<Map<String, Map<String, Frames>>>(markerStoragePath.readText())[symbol.simpleString()]!!
        } catch (_: Exception) {
            return emptyMap()
        }.mapValues { (key, value) -> Marker(value, key) }
    }

    fun calculateAnimationStats(animation: AnimationData? = activeAnimationData.value) {
        val freshAnimation = animation?.factory() ?: return
        var _animationLength = 0
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

    fun setPaused(paused: Boolean) {
        pausedFlow.value = paused
    }

    private val _cursorFrame = MutableStateFlow(1)
    val cursorFrame: StateFlow<Int> = _cursorFrame

    fun setCursorFrame(cursor: Int, force: Boolean = false) {
        val cursor = cursor.coerceIn(1..animationLength.value)
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
        seekAnimationTo(_cursorFrame.value + delta)
        _cursorFrame.value += delta
    }

    fun tickFrame() {
        if (pausedFlow.value) return
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

    fun setMarker(name: String, newValue: Marker) {
        println("$name = $newValue")
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
