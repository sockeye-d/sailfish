package dev.fishies.ranim2.containers

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import dev.fishies.ranim2.containers.Axis.Companion.get
import dev.fishies.ranim2.containers.LinearContainer.Properties
import dev.fishies.ranim2.core.CompositeElement
import dev.fishies.ranim2.core.Container
import dev.fishies.ranim2.core.Element
import dev.fishies.ranim2.core.attached

enum class Axis {
    X,
    Y;

    val opposite
        get() = when (this) {
            X -> Y
            Y -> X
        }

    companion object {
        operator fun Size.get(axis: Axis) = when (axis) {
            X -> width
            Y -> height
        }

        operator fun Offset.get(axis: Axis) = when (axis) {
            X -> x
            Y -> y
        }
    }
}

class LinearContainer(_axis: Axis, _separation: Float) : Container() {
    var axis by mutableStateOf(_axis)
    var separation by mutableStateOf(_separation)

    val crossAxis by derivedStateOf { axis.opposite }

    override val minimumSize by derivedStateOf {
        constructSize(
            layoutableChildren.foldIndexed(0.0f) { i, acc, e -> acc + e.minimumSize[axis] + (if (i == 0) 0.0f else separation) },
            layoutableChildren.maxOf { it.minimumSize[crossAxis] },
        )
    }

    override fun layout(childrenToLayout: List<Element>) {
        val childCount = childrenToLayout.size
        val totalSeparation = (childCount - 1) * separation
        val (alongAxis, acrossAxis) = size.convertXyMainCross()
        var fractionTotal = 0.0f
        var stretchMin = 0.0f
        var stretchAvailable = 0.0f

        val childSizes = ArrayList<ChildSize>(childrenToLayout.size)

        for (child in childrenToLayout) {
            val childAlongAxisSize = child.minimumSize[axis]
            val fraction = child.fraction
            val childSize = ChildSize(
                minimSize = childAlongAxisSize,
                finalSize = childAlongAxisSize,
                willStretch = fraction != null,
                fraction = fraction ?: 0.0f
            )
            stretchMin += childSize.minimSize
            if (childSize.willStretch) {
                stretchAvailable += childSize.minimSize
                fractionTotal += childSize.fraction
            }
            childSizes.add(childSize)
        }

        val stretchMax = alongAxis - totalSeparation
        val stretchDiff = (stretchMax - stretchMin).coerceAtLeast(0.0f)
        stretchAvailable += stretchDiff

        while (childSizes.any { it.willStretch }) {
            var refitSuccessful = true
            for (childSize in childSizes) {
                if (childSize.willStretch) {
                    val finalSize = stretchAvailable * childSize.fraction / fractionTotal
                    if (finalSize < childSize.minimSize) {
                        childSize.willStretch = false
                        fractionTotal -= childSize.fraction
                        refitSuccessful = false
                        stretchAvailable -= childSize.minimSize
                        childSize.finalSize = childSize.minimSize
                    } else {
                        childSize.finalSize = finalSize
                    }
                }
            }

            if (refitSuccessful) {
                break
            }
        }

        var first = true
        var offset = 0.0f

        for ((child, childSize) in childrenToLayout.zip(childSizes)) {
            if (first) {
                first = false
            } else {
                offset += separation
            }

            val rect = Rect(
                Offset(offset, 0.0f).convertXyMainCross(), Size(childSize.finalSize, acrossAxis).convertXyMainCross()
            )

            child.fitInRect(rect)
            offset += childSize.finalSize
        }

        //size = Size(size.width.coerceAtLeast(minimumSize.width), size.height.coerceAtLeast(minimumSize.height))
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun constructSize(alongAxis: Float, alongCrossAxis: Float) = when (axis) {
        Axis.X -> Size(alongAxis, alongCrossAxis)
        Axis.Y -> Size(alongCrossAxis, alongAxis)
    }

    /**
     * Converts a size in X-Y to and from main-cross coordinates.
     *
     * When called twice,
     * * if [axis] is [Axis.X]: nothing happens twice.
     * * if [axis] is [Axis.Y]: it gets swapped twice, thus nothing happens.
     */
    @Suppress("NOTHING_TO_INLINE")
    private inline fun Size.convertXyMainCross() = when (axis) {
        Axis.X -> Size(width, height)
        Axis.Y -> Size(height, width)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Offset.convertXyMainCross() = when (axis) {
        Axis.X -> Offset(x, y)
        Axis.Y -> Offset(y, x)
    }

    class Properties {
        var fraction: Float? by mutableStateOf(null)
    }
}

var Element.fraction by attached<_, _, LinearContainer>(Properties::fraction, default = { null })

internal data class ChildSize(
    val minimSize: Float = 0.0f,
    var finalSize: Float = 0.0f,
    var willStretch: Boolean = false,
    var fraction: Float = 0.0f,
)

fun CompositeElement.linearContainer(
    axis: Axis = Axis.X,
    separation: Float = 0.0f,
    contents: LinearContainer.() -> Unit,
) = LinearContainer(axis, separation).also(::addChild).apply(contents)
