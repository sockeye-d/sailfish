package dev.fishies.ranim2

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.fishies.ranim2.util.appendBlock
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

    fun propertyList(): Map<String, String> = mapOf(
        "position" to position.toString(),
        "size" to size.toString(),
        "visible" to visible.toString(),
    )

    val stringRepresentation: String
        get() = buildString {
            append(this@Element::class.simpleName)
            val properties = propertyList()
            if (properties.isNotEmpty()) {
                append("(")
                append(propertyList().entries.joinToString { (name, value) -> "${name}=${value}" })
                append(")")
            }
        }
}

fun Element.treeString(): String = buildString {
    val element = this@treeString
    append("$element ")
    appendBlock {
        for (child in children) {
            appendLine(child.treeString())
        }
    }
}
