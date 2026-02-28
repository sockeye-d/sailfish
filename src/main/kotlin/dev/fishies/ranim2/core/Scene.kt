package dev.fishies.ranim2.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

open class Scene : Element {
    override var visible by mutableStateOf(true)
    var children: List<Element> by mutableStateOf(emptyList())
    var transform by mutableStateOf(Matrix())

    override fun DrawScope.draw() {
        if (!visible) return

        withTransform({ transform(transform) }) {
            for (child in children) {
                with(child) { draw() }
            }
        }
    }

    fun addChild(element: Element) {
        children += element
    }

    fun addChild(elements: List<Element>) {
        children += elements
    }

    fun removeChild(elements: List<Element>) {
        children = children.filter { it !in elements }
    }
}
