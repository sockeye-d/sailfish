package dev.fishies.ranim2.gui.util

import dev.fishies.ranim2.ksp.AnimationMetadata
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.nio.file.*
import kotlin.io.path.inputStream
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalSerializationApi::class)
suspend fun FlowCollector<AnimationMetadata>.watchFile(watcher: WatchService, metadataPath: Path) {
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
