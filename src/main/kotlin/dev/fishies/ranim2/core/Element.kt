package dev.fishies.ranim2.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.reflect.KClass

interface Element {
    var attachedProperties: Map<KClass<*>, Any?>

    var children: List<Element>
    var parent: Element?
    var position: Offset
    var size: Size
    val minimumSize: Size
    var visible: Boolean
    fun DrawScope.draw()

    fun runLayoutPass()

    operator fun <T : Element> T.invoke(block: T.() -> Unit) = apply(block)
}

fun Element.treeString(): String = buildString {
    val element = this@treeString
    append("${element::class.simpleName}($element) ")
    appendBlock {
        for (child in children) {
            appendLine(child.treeString())
        }
    }
}
