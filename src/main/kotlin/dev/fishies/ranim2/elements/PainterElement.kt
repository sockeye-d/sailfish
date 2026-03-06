package dev.fishies.ranim2.elements

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import dev.fishies.ranim2.core.CompositeElement

class PainterElement(val painter: Painter, position: Offset, size: Size, rotation: Float, tint: Color?) :
    BasicElement(position, size) {
    var tint by mutableStateOf(tint)
    var rotation by mutableStateOf(rotation)

    override fun DrawScope.draw() {
        val realSize = this@PainterElement.size * density
        val realPosition = position * density
        withTransform({
            translate(realPosition.x, realPosition.y)
            rotate(rotation, Offset(realSize.width / 2, realSize.height / 2))
        }) {
            with(painter) {
                draw(realSize, colorFilter = tint?.let { ColorFilter.tint(it, BlendMode.SrcAtop) })
            }
        }
    }
}

fun CompositeElement.makePainter(
    painter: Painter,
    position: Offset = Offset.Zero,
    size: Size = painter.intrinsicSize,
    rotation: Float = 0.0f,
    tint: Color? = null,
) =
    PainterElement(painter, position, size, rotation, tint).also { addChild(it) }
