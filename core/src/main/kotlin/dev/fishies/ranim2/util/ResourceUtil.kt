package dev.fishies.ranim2.util

import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.Density
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.decodeToSvgPainter

private val thisClassLoader = object {}::class.java.classLoader

inline fun <reified T> loadJson(resourceUri: String, loader: ClassLoader) = Json.decodeFromString<T>(loadBytes(resourceUri, loader).decodeToString())

fun loadBytes(resourceUri: String, loader: ClassLoader): ByteArray {
    val stream = loader.getResourceAsStream(resourceUri) ?: error("Resource at $resourceUri not found")
    return stream.readBytes()
}

fun loadSvg(resourceUri: String, loader: ClassLoader) = loadBytes(resourceUri, loader).decodeToSvgPainter(Density(1.0f))

fun loadImage(resourceUri: String, loader: ClassLoader, filterQuality: FilterQuality = FilterQuality.Low) = BitmapPainter(
    loadBytes(resourceUri, loader).decodeToImageBitmap(), filterQuality = filterQuality
)

fun loadJsonElement(resourceUri: String, loader: ClassLoader) = Json.parseToJsonElement(loadBytes(resourceUri, loader).decodeToString())
