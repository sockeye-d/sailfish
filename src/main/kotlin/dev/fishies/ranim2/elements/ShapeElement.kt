package dev.fishies.ranim2.elements

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.*
import dev.fishies.ranim2.core.CompositeElement

class ShapeElement(shape: Shape, position: Offset, size: Size, rotation: Float, color: Color, style: DrawStyle) :
    BasicElement(position, size) {
    var color by mutableStateOf(color)
    var rotation by mutableStateOf(rotation)
    var style by mutableStateOf(style)
    var shape by mutableStateOf(shape)

    private val shapeOutline by derivedStateOf { this.shape.createOutline(this.size, LayoutDirection.Ltr, Density(1f)) }

    override fun DrawScope.draw() {
        val size = this@ShapeElement.size
        withTransform({
            translate(position.x, position.y)
            rotate(rotation, Offset(size.width / 2, size.height / 2))
        }) {
            drawOutline(shapeOutline, color, style = style)
        }
    }
}

fun CompositeElement.makeShape(
    shape: Shape,
    size: Size,
    color: Color,
    style: DrawStyle = Fill,
    position: Offset = Offset.Zero,
    rotation: Float = 0f,
) = ShapeElement(shape, position, size, rotation, color, style).also(this::addChild)

fun CompositeElement.makeCircle(
    size: Size,
    color: Color,
    style: DrawStyle = Fill,
    position: Offset = Offset.Zero,
    rotation: Float = 0f,
) = makeShape(CircleShape, size, color, style, position, rotation)

fun CompositeElement.makeRectangle(
    size: Size,
    color: Color,
    style: DrawStyle = Fill,
    position: Offset = Offset.Zero,
    radius: Float = 0f,
    rotation: Float = 0f,
) = makeShape(RoundedCornerShape(radius), size, color, style, position, rotation)
