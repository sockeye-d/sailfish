package dev.fishies.ranim2.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import kotlin.reflect.KMutableProperty

typealias Frames = Int

class TweenAnimation<T>(
    private val property: KMutableProperty<T>,
    private val from: T,
    private val to: T,
    private val length: Frames,
    private val animator: (from: T, to: T, factor: Double) -> T,
    private val tweener: (Double) -> Double,
) : Animation {
    private var tick: Int = 0

    override val isFinished: Boolean
        get() = tick > length

    override fun tick() {
        val time = tick.toDouble() / length.toDouble()
        val factor = tweener(time)
        property.setter.call(animator(from, to, factor))
        tick++
    }
}

fun <T : Any> KMutableProperty<T>.tween(
    from: T? = null,
    to: T,
    length: Frames,
    animator: (from: T, to: T, factor: Double) -> T,
    tweener: (Double) -> Double = { it },
) = TweenAnimation(
    property = this,
    from = from ?: this.getter.call(),
    to = to,
    length = length,
    animator = animator,
    tweener = tweener
)

fun KMutableProperty<Float>.tween(
    from: Float? = null,
    to: Float,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimation(
    property = this,
    from = from ?: this.getter.call(),
    to = to,
    length = length,
    animator = { from, to, factor -> from + (to - from) * factor.toFloat() },
    tweener = tweener
)

fun KMutableProperty<Double>.tween(
    from: Double? = null,
    to: Double,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimation(
    property = this,
    from = from ?: this.getter.call(),
    to = to,
    length = length,
    animator = { from, to, factor -> from + (to - from) * factor },
    tweener = tweener
)

fun KMutableProperty<Dp>.tween(
    from: Dp = Dp.Unspecified,
    to: Dp,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimation(
    property = this,
    from = if (from.isSpecified) from else this.getter.call(),
    to = to,
    length = length,
    animator = { from, to, factor -> lerp(from, to, factor.toFloat()) },
    tweener = tweener
)

fun KMutableProperty<DpOffset>.tween(
    from: DpOffset = DpOffset.Unspecified,
    to: DpOffset,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimation(
    property = this,
    from = if (from.isSpecified) from else this.getter.call(),
    to = to,
    length = length,
    animator = { from, to, factor -> lerp(from, to, factor.toFloat()) },
    tweener = tweener
)

fun KMutableProperty<Offset>.tween(
    from: Offset = Offset.Unspecified,
    to: Offset,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimation(
    property = this,
    from = if (from.isSpecified) from else this.getter.call(),
    to = to,
    length = length,
    animator = { from, to, factor -> lerp(from, to, factor.toFloat()) },
    tweener = tweener
)

fun KMutableProperty<Color>.tween(
    from: Color = Color.Unspecified,
    to: Color,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimation(
    property = this,
    from = if (from.isSpecified) from else this.getter.call(),
    to = to,
    length = length,
    animator = { from, to, factor -> lerp(from, to, factor.toFloat()) },
    tweener = tweener
)

fun KMutableProperty<Size>.tween(
    from: Size = Size.Unspecified,
    to: Size,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimation(
    property = this,
    from = if (from.isSpecified) from else this.getter.call(),
    to = to,
    length = length,
    animator = { from, to, factor -> lerp(from, to, factor.toFloat()) },
    tweener = tweener
)

fun KMutableProperty<Matrix>.tween(
    from: Matrix? = null,
    to: Matrix,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimation(
    property = this,
    from = from ?: this.getter.call(),
    to = to,
    length = length,
    animator = { from, to, factor -> Matrix(from.values.zip(to.values).map { (a, b) -> lerp(a, b, factor.toFloat()) }.toFloatArray()) },
    tweener = tweener
)
