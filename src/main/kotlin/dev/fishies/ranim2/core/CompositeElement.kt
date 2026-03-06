package dev.fishies.ranim2.core

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

open class CompositeElement : Element {
    override var parent: Element? by mutableStateOf(null)
    override var position by mutableStateOf(Offset.Zero)
    override var size: Size = Size.Unspecified
    override val minimumSize: Size
        get() = Size(children.maxOf { it.minimumSize.width }, children.maxOf { it.minimumSize.height })
    override var visible by mutableStateOf(true)
    var children by mutableStateOf(emptyList<Element>())
        protected set
    var transform by mutableStateOf(Matrix())

    protected val finalTransform by derivedStateOf {
        Matrix(transform.values.clone()).apply {
            translate(position.x, position.y)
        }
    }

    override fun DrawScope.draw() {
        if (!visible) return

        withTransform({ transform(finalTransform) }) {
            for (child in children.filter(Element::visible)) {
                with(child) { draw() }
            }
        }
    }

    protected open fun actuallyDoTheLayout() {}

    override fun runLayoutPass() {
        for (child in children) {
            child.runLayoutPass()
        }

        actuallyDoTheLayout()
    }

    open fun addChild(element: Element) {
        children += element
        element.parent = this
    }

    open fun addChild(elements: List<Element>) {
        children += elements
        elements.forEach { it.parent = this }
    }

    open fun removeChild(element: Element) {
        children = children.filter { it != element }
        element.parent = null
    }

    open fun removeChild(elements: List<Element>) {
        children = children.filter { it !in elements }
        elements.forEach { it.parent = null }
    }
}
