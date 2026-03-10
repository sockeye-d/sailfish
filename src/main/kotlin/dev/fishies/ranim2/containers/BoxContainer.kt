package dev.fishies.ranim2.containers

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.util.lerp
import dev.fishies.ranim2.core.CompositeElement
import dev.fishies.ranim2.core.Container
import dev.fishies.ranim2.core.Element
import dev.fishies.ranim2.core.attached
import dev.fishies.ranim2.core.minus
import dev.fishies.ranim2.core.toOffset
import dev.fishies.ranim2.core.times
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class BoxContainer : Container() {
    override val minimumSize by derivedStateOf {
        Size(
            layoutableChildren.maxOf { it.minimumSize.width },
            layoutableChildren.maxOf { it.minimumSize.height },
        )
    }

    override fun layout(childrenToLayout: List<Element>) {
        for (child in childrenToLayout) {
            val childMinSize = child.minimumSize
            val childAnchor = child.anchor
            val targetSize = Size(
                lerp(childMinSize.width, size.width, childAnchor.x.fillFactor),
                lerp(childMinSize.height, size.height, childAnchor.y.fillFactor),
            )
            val targetOffset = (size - targetSize).toOffset() * childAnchor.factor()
            child.fitInRect(targetOffset, targetSize)
        }
    }

    class Properties {
        var align by mutableStateOf(Anchor.fill)
    }
}

var Element.anchor by attached<_, _, BoxContainer>(BoxContainer.Properties::align, default = Anchor::fill)

open class AxisAnchor(val factor: Float, val fillFactor: Float = 0.0f) {
    data object Start : AxisAnchor(0.0f)
    data object Middle : AxisAnchor(0.5f)
    data object End : AxisAnchor(1.0f)
    data object Fill : AxisAnchor(0.5f, fillFactor = 1.0f)
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
        val t = Anchor(AxisAnchor.Fill, AxisAnchor.Start)
        val m = Anchor(AxisAnchor.Fill, AxisAnchor.Middle)
        val b = Anchor(AxisAnchor.Fill, AxisAnchor.End)
    }
    object Tall {
        val t = Anchor(AxisAnchor.Start, AxisAnchor.Fill)
        val m = Anchor(AxisAnchor.Middle, AxisAnchor.Fill)
        val b = Anchor(AxisAnchor.End, AxisAnchor.Fill)
    }
}

fun Anchor.factor() = Offset(x.factor, y.factor)

fun lerp(start: AxisAnchor, stop: AxisAnchor, fraction: Float) = AxisAnchor(
    lerp(start.factor, stop.factor, fraction),
    lerp(start.fillFactor, stop.fillFactor, fraction),
)

fun lerp(start: Anchor, stop: Anchor, fraction: Float) =
    Anchor(lerp(start.x, stop.x, fraction), lerp(start.y, stop.y, fraction))

@OptIn(ExperimentalContracts::class)
fun CompositeElement.boxContainer(
    contents: BoxContainer.() -> Unit,
): BoxContainer {
    contract {
        callsInPlace(contents, InvocationKind.EXACTLY_ONCE)
    }
    return BoxContainer().also(::addChild).apply(contents)
}
