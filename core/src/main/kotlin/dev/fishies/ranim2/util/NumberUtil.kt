package dev.fishies.ranim2.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import dev.fishies.ranim2.Element
import dev.fishies.ranim2.languages.common.TreeSitterLanguage
import dev.fishies.ranim2.syntax.highlightToAnnotations
import dev.fishies.ranim2.theming.theme
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.exp
import kotlin.math.pow

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

inline fun StringBuilder.appendBlock(
    opening: String = "{",
    closing: String = "}",
    indent: String = "    ",
    block: StringBuilder.() -> Unit,
) {
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

inline fun <reified T> stepLerp(from: T, to: T, factor: Float) = if (factor < 0.5f) from else to

fun lerp(start: Color, stop: Color, fraction: Float, colorSpace: ColorSpace = ColorSpaces.LinearSrgb): Color {
    val startColor = start.convert(colorSpace)
    val endColor = stop.convert(colorSpace)

    val startAlpha = startColor.alpha
    val startL = startColor.red
    val startA = startColor.green
    val startB = startColor.blue

    val endAlpha = endColor.alpha
    val endL = endColor.red
    val endA = endColor.green
    val endB = endColor.blue

    // We need to clamp the input fraction since over/undershoot easing curves
    // can yield fractions outside of the 0..1 range, which would in turn cause
    // Lab/alpha values to be outside of the valid color range.
    // Clamping the fraction is cheaper than clamping all 4 components separately.
    val t = fraction.fastCoerceIn(0.0f, 1.0f)
    val interpolated = Color(
        lerp(startL, endL, t),
        lerp(startA, endA, t),
        lerp(startB, endB, t),
        lerp(startAlpha, endAlpha, t),
        colorSpace,
    )
    return interpolated.convert(stop.colorSpace)
}

/**
 * [delta] is in seconds per frame
 */
fun expDamp(source: Float, target: Float, smoothing: Float, delta: Float) =
    lerp(target, source, exp(-smoothing * delta))

fun exp10(power: Int) = when (power) {
    0 -> 1
    1 -> 10
    2 -> 100
    3 -> 1000
    4 -> 10000
    5 -> 100000
    6 -> 1000000
    7 -> 10000000
    8 -> 100000000
    9 -> 1000000000
    else -> 10.0.pow(power).toInt()
}
