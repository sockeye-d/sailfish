package dev.fishies.ranim2.util

import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.Density
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.decodeToSvgPainter
import java.net.URI

private val loaderClass = object {}::class.java

fun loadBytes(resourceUri: String): ByteArray = URI.create(resourceUri).toURL().readBytes()

fun loadSvg(resourceUri: String) = loadBytes(resourceUri).decodeToSvgPainter(Density(1.0f))

fun loadImage(resourceUri: String, filterQuality: FilterQuality = FilterQuality.Low) = BitmapPainter(
    loadBytes(resourceUri).decodeToImageBitmap(), filterQuality = filterQuality
)

fun loadJsonElement(resourceUri: String) = Json.parseToJsonElement(loadBytes(resourceUri).decodeToString())
