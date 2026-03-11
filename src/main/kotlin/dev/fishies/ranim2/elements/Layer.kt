package dev.fishies.ranim2.elements

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.SkiaGraphicsContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.roundToIntSize
import dev.fishies.ranim2.core.CompositeElement
import dev.fishies.ranim2.core.Element
import dev.fishies.ranim2.core.attached
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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

    class Properties {
        @OptIn(InternalComposeUiApi::class)
        var graphicsContext: GraphicsContext by mutableStateOf(SkiaGraphicsContext())
    }
}

@OptIn(InternalComposeUiApi::class)
private val defaultGraphicsContext by lazy { SkiaGraphicsContext() }

@OptIn(InternalComposeUiApi::class)
var Element.graphicsContext by attached<_, _, Element>(Layer.Properties::graphicsContext, recursive = true) { defaultGraphicsContext }

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
