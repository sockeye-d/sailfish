package dev.fishies.ranim2.core

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.lerp
import dev.fishies.ranim2.containers.Anchor
import dev.fishies.ranim2.containers.lerp
import kotlin.reflect.KMutableProperty0

typealias Frames = Int

class TweenAnimated<T>(
    private val property: KMutableProperty0<T>,
    private val from: T,
    private val to: T,
    private val length: Frames,
    private val animator: (from: T, to: T, factor: Double) -> T,
    private val tweener: (Double) -> Double,
) : Animated {
    private var tick: Int = 0

    override val isFinished: Boolean
        get() = tick > length

    override fun tick() {
        val time = tick.toDouble() / length.toDouble()
        val factor = tweener(time)
        property.setter.call(animator(from, to, factor))
        tick++
    }

    override fun toString() = "TweenAnimation($property from $from to $to in $length frames)"
}

fun <T : Any> KMutableProperty0<T>.tween(
    from: T? = null,
    to: T,
    length: Frames,
    animator: (from: T, to: T, factor: Double) -> T,
    tweener: (Double) -> Double = { it },
) = TweenAnimated(
    property = this, from = from ?: this.getter.call(), to = to, length = length, animator = animator, tweener = tweener
)

fun KMutableProperty0<Float>.tween(
    from: Float? = null,
    to: Float,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimated(
    property = this,
    from = from ?: this.getter.call(),
    to = to,
    length = length,
    animator = { from, to, factor -> from + (to - from) * factor.toFloat() },
    tweener = tweener
)

/**
 * Please don't use on null properties, or provide a [from].
 */
@JvmName("tweenFloatNull")
fun KMutableProperty0<Float?>.tween(
    from: Float? = null,
    to: Float,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimated(
    property = this,
    from = from ?: this.getter.call()!!,
    to = to,
    length = length,
    animator = { from, to, factor -> from!! + (to!! - from) * factor.toFloat() },
    tweener = tweener
)

fun KMutableProperty0<Double>.tween(
    from: Double? = null,
    to: Double,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimated(
    property = this,
    from = from ?: this.getter.call(),
    to = to,
    length = length,
    animator = { from, to, factor -> from + (to - from) * factor },
    tweener = tweener
)

fun KMutableProperty0<Dp>.tween(
    from: Dp = Dp.Unspecified,
    to: Dp,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimated(
    property = this,
    from = if (from.isSpecified) from else this.getter.call(),
    to = to,
    length = length,
    animator = { from, to, factor -> lerp(from, to, factor.toFloat()) },
    tweener = tweener
)

fun KMutableProperty0<DpOffset>.tween(
    from: DpOffset = DpOffset.Unspecified,
    to: DpOffset,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimated(
    property = this,
    from = if (from.isSpecified) from else this.getter.call(),
    to = to,
    length = length,
    animator = { from, to, factor -> lerp(from, to, factor.toFloat()) },
    tweener = tweener
)

fun KMutableProperty0<Offset>.tween(
    from: Offset = Offset.Unspecified,
    to: Offset,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimated(
    property = this,
    from = if (from.isSpecified) from else this.getter.call(),
    to = to,
    length = length,
    animator = { from, to, factor -> lerp(from, to, factor.toFloat()) },
    tweener = tweener
)

fun KMutableProperty0<Color>.tween(
    from: Color = Color.Unspecified,
    to: Color,
    length: Frames,
    colorSpace: ColorSpace = ColorSpaces.LinearSrgb,
    tweener: (Double) -> Double = { it },
) = TweenAnimated(
    property = this,
    from = if (from.isSpecified) from else this.getter.call(),
    to = to,
    length = length,
    animator = { from, to, factor -> lerp(from, to, factor.toFloat(), colorSpace) },
    tweener = tweener
)

fun KMutableProperty0<Size>.tween(
    from: Size = Size.Unspecified,
    to: Size,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimated(
    property = this,
    from = if (from.isSpecified) from else this.getter.call(),
    to = to,
    length = length,
    animator = { from, to, factor -> lerp(from, to, factor.toFloat()) },
    tweener = tweener
)

fun KMutableProperty0<Anchor>.tween(
    from: Anchor? = null,
    to: Anchor,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimated(
    property = this,
    from = from ?: this.getter.call(),
    to = to,
    length = length,
    animator = { from, to, factor -> lerp(from, to, factor.toFloat()) },
    tweener = tweener
)

fun KMutableProperty0<Matrix>.tween(
    from: Matrix? = null,
    to: Matrix,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimated(
    property = this, from = from ?: this.getter.call(), to = to, length = length, animator = { from, to, factor ->
        Matrix(from.values.zip(to.values).map { (a, b) -> lerp(a, b, factor.toFloat()) }.toFloatArray())
    }, tweener = tweener
)
