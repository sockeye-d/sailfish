package dev.fishies.ranim2.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.WindowPosition.PlatformDefault.x
import androidx.compose.ui.window.WindowPosition.PlatformDefault.y
import dev.fishies.ranim2.languages.common.TreeSitterLanguage
import dev.fishies.ranim2.syntax.highlightToAnnotations
import dev.fishies.ranim2.theming.theme
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.decodeToSvgPainter
import java.net.URI
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun Size.coerceAtLeast(minimumSize: Size) =
    Size(width.coerceAtLeast(minimumSize.width), height.coerceAtLeast(minimumSize.height))

fun Size.coerceAtMost(maximumSize: Size) =
    Size(width.coerceAtMost(maximumSize.width), height.coerceAtMost(maximumSize.height))

fun Color.toHtmlColor(small: Boolean = false, alpha: Boolean = this.alpha != 1.0f) = buildString {
    fun convertInt(int: Int) {
        val int = int.coerceIn(0, 255)
        append(int.toString(16).slice(if (small) 0..<1 else 0..<2))
    }
    append("#")
    val argb = toArgb()
    append(convertInt(argb shr 4 and 0xFF))
    append(convertInt(argb shr 2 and 0xFF))
    append(convertInt(argb and 0xFF))
    if (alpha) {
        append(convertInt(argb shr 6 and 0xFF))
    }
}

private val validHtmlColorLengths = arrayOf(3, 4, 6, 8)

fun String.fromHtmlColor(): Color {
    val string = removePrefix("#")
    require(string.length in validHtmlColorLengths) { "Invalid html color $this" }
    val isShort = string.length == 3 || string.length == 4
    val hasAlpha = string.length == 4 || string.length == 8
    val maxValue = if (isShort) 15f else 255f
    val componentLength = if (isShort) 1 else 2
    val red = string.substring(0, componentLength * 1).toInt(16) / maxValue
    val green = string.substring(componentLength * 1, componentLength * 2).toInt(16) / maxValue
    val blue = string.substring(componentLength * 2, componentLength * 3).toInt(16) / maxValue
    val alpha = if (hasAlpha) string.substring(componentLength * 2, componentLength).toInt(16) / maxValue else 1.0f
    return Color(red, green, blue, alpha)
}

private val loaderClass = object {}::class.java

fun loadBytes(resourceUri: String): ByteArray = URI.create(resourceUri).toURL().readBytes()

fun loadSvg(resourceUri: String) = loadBytes(resourceUri).decodeToSvgPainter(Density(1.0f))

fun loadImage(resourceUri: String, filterQuality: FilterQuality = FilterQuality.Low) = BitmapPainter(
    loadBytes(resourceUri).decodeToImageBitmap(), filterQuality = filterQuality
)

fun loadJsonElement(resourceUri: String) = Json.parseToJsonElement(loadBytes(resourceUri).decodeToString())

inline fun <reified T> loadJson(resourceUri: String) = Json.decodeFromString<T>(loadBytes(resourceUri).decodeToString())

@Suppress("NOTHING_TO_INLINE")
inline operator fun Size.minus(other: Size) = Size(width - other.width, height - other.height)

@Suppress("NOTHING_TO_INLINE")
inline operator fun Size.plus(other: Size) = Size(width + other.width, height + other.height)

@Suppress("NOTHING_TO_INLINE")
inline fun Size.toOffset() = Offset(width, height)

@Suppress("NOTHING_TO_INLINE")
inline operator fun Offset.times(other: Offset) = Offset(x * other.x, y * other.y)

context(element: Element)
inline val TreeSitterLanguage.Highlightable.highlighter
    get() = { text: String -> highlightToAnnotations(text, element.theme.syntax) }

inline fun StringBuilder.appendBlock(opening: String = "{", closing: String = "}", indent: String = "    ", block: StringBuilder.() -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val content = buildString(block).trimEnd()
    if (content.isBlank()) {
        append("$opening$closing")
        return
    }
    appendLine(opening)
    appendLine(content.prependIndent(indent))
    append(closing)
}
