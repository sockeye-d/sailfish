package dev.fishies.ranim2.core

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.*

abstract class Container : CompositeElement() {
    private var _size by mutableStateOf(Size.Zero)
    override var size
        get() = _size.coerceAtLeast(minimumSize)
        set(value) {
            _size = value
        }
    abstract override val minimumSize: Size

    abstract fun layout(childrenToLayout: List<Element>)

    override fun actuallyDoTheLayout() {
        layout(layoutableChildren)
    }

    val layoutableChildren by derivedStateOf { children.filter(Element::visible) }

    override fun DrawScope.draw() {
        if (!visible) return

        withTransform({ transform(finalTransform) }) {
            for (child in layoutableChildren) {
                with(child) { draw() }
            }
        }

        if (drawContainerOutlines) {
            drawRect(Color.White, position, this@Container.size, style = Stroke(width = 1.0f))
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun Element.fitInRect(rect: Rect) {
        position = rect.topLeft
        size = rect.size
    }

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun Element.fitInRect(position: Offset, size: Size) {
        this.position = position
        this.size = size
    }

    companion object {
        var drawContainerOutlines by mutableStateOf(false)
    }
}
