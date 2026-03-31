package dev.fishies.sailfish

import androidx.compose.runtime.Immutable
import dev.fishies.sailfish.Markers.storage

@Immutable
data class Marker(
    val position: Frames,
    val name: String,
)

interface MarkerStorage {
    operator fun get(name: String): Marker?
}

object Markers {
    lateinit var storage: MarkerStorage
}

suspend fun Animation.yield(marker: String) {
    val position = markerPosition(marker)
    while (absoluteTicks <= position) {
        yield()
    }
}

fun Animation.isPast(marker: String): Boolean {
    val position = markerPosition(marker)
    return absoluteTicks > position
}

private fun markerPosition(marker: String): Frames = requireNotNull(storage[marker]) { "Marker '$marker' not found" }.position
