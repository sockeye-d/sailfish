package dev.fishies.ranim2.gui.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.FlowCollector
import java.nio.file.*
import kotlin.io.path.absolutePathString
import kotlin.time.Duration.Companion.milliseconds

suspend fun FlowCollector<Path>.watchFile(watcher: WatchService, metadataPath: Path) {
    val registeredPath = metadataPath.parent
    withContext(Dispatchers.IO) {
        registeredPath.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE)
    }

    while (true) {
        var key: WatchKey? = null
        while (key == null) {
            key = watcher.poll()
            delay(1.milliseconds)
        }

        for (event in key.pollEvents()) {
            val kind = event.kind()
            if (kind !in listOf(StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE)) {
                continue
            }
            val path = event.context() as? Path ?: continue
            if (registeredPath.resolve(path).absolutePathString() != metadataPath.absolutePathString()) {
                println("$path != $metadataPath")
                continue
            }
            emit(path)
        }

        key.reset()
    }
}
