package dev.fishies.ranim2.containers

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.lerp
import dev.fishies.ranim2.CompositeElement
import dev.fishies.ranim2.Container
import dev.fishies.ranim2.Element
import dev.fishies.ranim2.Padding
import dev.fishies.ranim2.attached
import dev.fishies.ranim2.core.*
import dev.fishies.ranim2.elements.rectangle
import dev.fishies.ranim2.theming.*
import dev.fishies.ranim2.util.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class BoxContainer(_padding: Padding = zero) : Container() {
    override val minimumSize by derivedStateOf {
        (if (children.isEmpty()) Size.Zero else Size(
            layoutableChildren.maxOf { it.minimumSize.width },
            layoutableChildren.maxOf { it.minimumSize.height },
        )) + padding.size
    }

    var padding by mutableStateOf(_padding)

    override fun layout(childrenToLayout: List<Element>) {
        val paddedOffset = Offset(
            padding.left,
            padding.top,
        )

        val paddedSize = size - padding.size

        for (child in childrenToLayout) {
            val childMinSize = child.minimumSize
            val childAnchor = child.anchor
            val respectsPadding = child.respectsPadding
            val thisSize = if (respectsPadding) paddedSize else size
            val thisOffset = if (respectsPadding) paddedOffset else Offset.Zero
            val targetSize = Size(
                lerp(childMinSize.width, thisSize.width, childAnchor.x.fillFactor),
                lerp(childMinSize.height, thisSize.height, childAnchor.y.fillFactor),
            )
            val targetOffset = thisOffset + (thisSize - targetSize).toOffset() * childAnchor.factor()
            child.fitInRect(targetOffset, targetSize)
        }
    }

    class Properties {
        var align by mutableStateOf(Anchor.fill)
        var respectsPadding by mutableStateOf(true)
    }
}

var Element.anchor by attached<_, _, BoxContainer>(BoxContainer.Properties::align, default = Anchor::fill)
var Element.respectsPadding by attached<_, _, BoxContainer>(
    BoxContainer.Properties::respectsPadding,
    default = { true })

sealed class AxisAnchor(open val factor: Float, open val fillFactor: Float = 0.0f) {
    data object Start : AxisAnchor(0.0f)
    data object Middle : AxisAnchor(0.5f)
    data object End : AxisAnchor(1.0f)
    data object Fill : AxisAnchor(0.5f, fillFactor = 1.0f)
    data class Absolute(override val factor: Float, override val fillFactor: Float = 0.0f) : AxisAnchor(factor, fillFactor)
}

data class Anchor(
    val x: AxisAnchor,
    val y: AxisAnchor,
) {
    companion object {
        val fill = Anchor(AxisAnchor.Fill, AxisAnchor.Fill)
        val center = Anchor(AxisAnchor.Middle, AxisAnchor.Middle)
    }

    object Shrink {
        val tl = Anchor(AxisAnchor.Start, AxisAnchor.Start)
        val tm = Anchor(AxisAnchor.Middle, AxisAnchor.Start)
        val tr = Anchor(AxisAnchor.End, AxisAnchor.Start)
        val ml = Anchor(AxisAnchor.Start, AxisAnchor.Middle)
        val mm = Anchor(AxisAnchor.Middle, AxisAnchor.Middle)
        val mr = Anchor(AxisAnchor.End, AxisAnchor.Middle)
        val bl = Anchor(AxisAnchor.Start, AxisAnchor.End)
        val bm = Anchor(AxisAnchor.Middle, AxisAnchor.End)
        val br = Anchor(AxisAnchor.End, AxisAnchor.End)
    }

    object Wide {
        val top = Anchor(AxisAnchor.Fill, AxisAnchor.Start)
        val middle = Anchor(AxisAnchor.Fill, AxisAnchor.Middle)
        val bottom = Anchor(AxisAnchor.Fill, AxisAnchor.End)
    }

    object Tall {
        val left = Anchor(AxisAnchor.Start, AxisAnchor.Fill)
        val middle = Anchor(AxisAnchor.Middle, AxisAnchor.Fill)
        val right = Anchor(AxisAnchor.End, AxisAnchor.Fill)
    }
}

fun Anchor.factor() = Offset(x.factor, y.factor)

fun lerp(start: AxisAnchor, stop: AxisAnchor, fraction: Float) = AxisAnchor.Absolute(
    lerp(start.factor, stop.factor, fraction),
    lerp(start.fillFactor, stop.fillFactor, fraction),
)

fun lerp(start: Anchor, stop: Anchor, fraction: Float) =
    Anchor(lerp(start.x, stop.x, fraction), lerp(start.y, stop.y, fraction))

fun CompositeElement.box(
    padding: Padding = zero,
    contents: BoxContainer.() -> Unit,
): BoxContainer {
    contract {
        callsInPlace(contents, InvocationKind.EXACTLY_ONCE)
    }
    return BoxContainer(padding).also(::addChild).apply(contents)
}

fun CompositeElement.panel(
    bgColor: ThemeColor = Background2, radius: Float = 0.0f, padding: Padding = zero, contents: BoxContainer.() -> Unit
): BoxContainer {
    contract {
        callsInPlace(contents, InvocationKind.EXACTLY_ONCE)
    }
    return box {
        backgroundColor = bgColor
        rectangle(Size.Zero, theme[bgColor], radius = radius)() {
            anchor = fill
            respectsPadding = false
        }
        contents()
    }
}

fun CompositeElement.panel(
    bgColor: Color, radius: Float = 0.0f, padding: Padding = zero, contents: BoxContainer.() -> Unit
): BoxContainer {
    contract {
        callsInPlace(contents, InvocationKind.EXACTLY_ONCE)
    }
    return box {
        rectangle(Size.Zero, bgColor, radius = radius)() {
            anchor = fill
            respectsPadding = false
        }
        contents()
    }
}
