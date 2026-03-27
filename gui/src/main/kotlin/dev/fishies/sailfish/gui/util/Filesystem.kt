package dev.fishies.sailfish.gui.util

import kotlinx.coroutines.*
import java.nio.file.*
import kotlin.io.path.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val fs by lazy {
    FileSystems.getDefault()
}

private suspend fun WatchService.nextKey(pollingRate: Duration = 10.milliseconds): WatchKey {
    while (true) {
        delay(pollingRate)
        return poll() ?: continue
    }
}

/**
 * Watches the file at [filePath] for changes.
 */
suspend fun watchFile(filePath: Path, watcher: WatchService = fs.newWatchService(), onChange: suspend (Path) -> Unit): Nothing {
    val registeredPath = filePath.parent
    withContext(Dispatchers.IO) {
        registeredPath.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE)
    }

    while (true) {
        val key = watcher.nextKey()

        for (event in key.pollEvents()) {
            val kind = event.kind()
            if (kind !in listOf(StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE)) {
                continue
            }
            val path = registeredPath.resolve(event.context() as? Path ?: continue)
            if (path.absolutePathString() != filePath.absolutePathString()) {
                continue
            }
            onChange(path)
        }

        key.reset()
    }
}

suspend fun watchDir(
    path: Path,
    vararg ignorePaths: Path,
    watcher: WatchService = fs.newWatchService(),
    onChange: suspend (Path, kind: WatchEvent.Kind<*>) -> Unit
): Nothing {
    val keys = mutableMapOf<WatchKey, Path>()
    val ignorePathsAbsolute = ignorePaths.map { it.absolutePathString() }.toSet()
    fun shouldIgnorePath(path: Path) = path.isHidden() || path.normalize().absolutePathString() in ignorePathsAbsolute
    fun watchSubdir(path: Path) {
        if (shouldIgnorePath(path)) return
        val key = path.register(
            watcher,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE,
        )
        keys[key] = path
    }
    path.walk(PathWalkOption.INCLUDE_DIRECTORIES).filter { it.isDirectory() }.forEach(::watchSubdir)

    while (true) {
        val key = watcher.nextKey()
        val baseDir = keys[key] ?: error("This shouldn't happen")
        for (event in key.pollEvents().filterNotNull()) {
            val path: Path = baseDir.resolve(event.context() as? Path ?: continue)
            if (shouldIgnorePath(path)) continue
            val kind: WatchEvent.Kind<*> = event.kind()
            if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                val dirKey =
                    keys.filterValues { it.absolutePathString() == path.absolutePathString() }.keys.firstOrNull()
                if (dirKey != null) {
                    // This deletion referenced a folder, remove it from the listenable keys.
                    keys.remove(dirKey)
                    dirKey.cancel()
                }
            } else if (path.isDirectory() && kind == StandardWatchEventKinds.ENTRY_CREATE) watchSubdir(path)
            onChange(path, kind)
        }
        if (!key.reset()) {
            keys.remove(key)
        }
    }
}
