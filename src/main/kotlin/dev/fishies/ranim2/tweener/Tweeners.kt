package dev.fishies.ranim2.tweener

import kotlin.math.abs
import kotlin.math.pow

open class Style(
    val transformIn: (Double) -> Double,
    val transformOut: (transformedFactor: Double, originalFactor: Double) -> Double,
)

object In : Style({ it }, { it, _ -> it })

object Out : Style({ 1.0 - it }, { it, _ -> 1.0 - it })

object InOut :
    Style({ 1.0 - abs(it * 2.0 - 1.0) }, { it, original -> if (original < 0.5) it * 0.5 else 1.0 - it * 0.5 })

fun styledTweener(style: Style, base: (Double) -> Double) =
    { factor: Double -> style.transformOut(base(style.transformIn(factor)), factor) }

fun powTweener(power: Double, style: Style) = styledTweener(style) { it.pow(power) }

val linear = { factor: Double -> factor }
fun quadratic(style: Style) = powTweener(2.0, style)
fun cubic(style: Style) = powTweener(3.0, style)
fun quartic(style: Style) = powTweener(4.0, style)
fun quintic(style: Style) = powTweener(5.0, style)

fun exponential(style: Style) = styledTweener(style) {
    if (it == 0.0) {
        0.0
    } else {
        2.0.pow(10 * it - 10)
    }
}
