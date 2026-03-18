package dev.fishies.ranim2.util

import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.Density
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.decodeToSvgPainter

private val loaderClass = object {}::class.java

fun loadBytes(resourceUri: String): ByteArray {
    val loader = Thread.currentThread().contextClassLoader ?: error("Context class loader not set")
    val stream = loader.getResourceAsStream(resourceUri) ?: error("Resource at $resourceUri not found")
    return stream.readBytes()
}

fun loadSvg(resourceUri: String) = loadBytes(resourceUri).decodeToSvgPainter(Density(1.0f))

fun loadImage(resourceUri: String, filterQuality: FilterQuality = FilterQuality.Low) = BitmapPainter(
    loadBytes(resourceUri).decodeToImageBitmap(), filterQuality = filterQuality
)

fun loadJsonElement(resourceUri: String) = Json.parseToJsonElement(loadBytes(resourceUri).decodeToString())
