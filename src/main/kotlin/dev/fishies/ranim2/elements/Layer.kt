package dev.fishies.ranim2.elements

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.util.lerp
import dev.fishies.ranim2.core.*
import org.jetbrains.skia.ImageFilter
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class Layer(context: GraphicsContext, position: Offset, size: Size) : CompositeElement(position, size) {
    val graphicsLayer by derivedStateOf { context.createGraphicsLayer() }
    var renderEffect: RenderEffect? by mutableStateOf(null)
    var colorFilter: ColorFilter? by mutableStateOf(null)
    var alpha by mutableStateOf(1.0f)

    override fun DrawScope.draw() {
        val layerSize = if (this@Layer.size.isSpecified) this@Layer.size else size
        graphicsLayer.record(layerSize.roundToIntSize()) {
            for (child in children.filter(Element::visible)) {
                with(child) { draw() }
            }
        }
        graphicsLayer.renderEffect = renderEffect
        graphicsLayer.colorFilter = colorFilter
        graphicsLayer.alpha = alpha
        drawLayer(graphicsLayer)
    }

    override fun propertyList() = super.propertyList() + mapOf(
        "renderEffect" to renderEffect.toString(),
        "colorFilter" to colorFilter.toString(),
        "alpha" to alpha.toString(),
    )

    class Properties {
        @OptIn(InternalComposeUiApi::class)
        var graphicsContext: GraphicsContext by mutableStateOf(SkiaGraphicsContext())
    }
}

@OptIn(InternalComposeUiApi::class)
private val defaultGraphicsContext by lazy { SkiaGraphicsContext() }

@OptIn(InternalComposeUiApi::class)
var Element.graphicsContext by attached<_, _, Element>(
    Layer.Properties::graphicsContext, recursive = true
) { defaultGraphicsContext }

fun CompositeElement.layer(
    graphicsContext: GraphicsContext = this.graphicsContext,
    position: Offset = Offset.Zero,
    size: Size = Size.Unspecified,
    contents: Layer.() -> Unit,
): Layer {
    contract {
        callsInPlace(contents, InvocationKind.EXACTLY_ONCE)
    }
    return Layer(graphicsContext, position, size).also { addChild(it) }.apply(contents)
}

fun KMutableProperty0<RenderEffect?>.tween(
    from: RenderEffect? = null,
    to: RenderEffect? = null,
    length: Frames,
    tweener: (Double) -> Double = { it },
) = TweenAnimated(
    property = this, from = from ?: this.getter.call(), to = to, length = length, animator = { from, to, factor ->
        lerpRenderEffect(from, to, factor.toFloat())
    }, tweener = tweener
)

/**
 * They really shouldn't make these fields private but whatever.
 */
private object BlurFields {
    val renderEffect by findProperty<BlurEffect, RenderEffect?>("renderEffect")
    val radiusX by findProperty<BlurEffect, Float>("radiusX")
    val radiusY by findProperty<BlurEffect, Float>("radiusY")
    val edgeTreatment by findProperty<BlurEffect, TileMode>("edgeTreatment")

    @Suppress("UNCHECKED_CAST")
    private fun <K, T> findProperty(name: String) = lazy {
        BlurEffect::class.memberProperties.find { it.name == name }!!.apply { isAccessible = true } as KProperty1<K, T>
    }
}

private fun lerpRenderEffect(
    from: RenderEffect?,
    to: RenderEffect?,
    factor: Float,
): RenderEffect? {
    if (factor < 0.001f) return from
    if (factor > 0.99f) return to
    if (from == to) {
        return from
    }

    if (from is BlurEffect && to is BlurEffect) return BlurEffect(
        stepLerp(BlurFields.renderEffect(from), BlurFields.renderEffect(from), factor),
        lerp(BlurFields.radiusX(from), BlurFields.radiusX(to), factor),
        lerp(BlurFields.radiusY(from), BlurFields.radiusY(to), factor),
        stepLerp(BlurFields.edgeTreatment(from), BlurFields.edgeTreatment(from), factor)
    )

    // Simple lerp for unrecognized filters
    return ImageFilter.makeArithmetic(
        k1 = 0.0f,
        k2 = factor,
        k3 = 1.0f - factor,
        k4 = 0.0f,
        enforcePMColor = false,
        bg = from?.asSkiaImageFilter(),
        fg = to?.asSkiaImageFilter(),
        crop = null
    ).asComposeRenderEffect()
}
