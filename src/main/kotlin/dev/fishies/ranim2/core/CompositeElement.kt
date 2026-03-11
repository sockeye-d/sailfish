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
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.full.createInstance
import kotlin.to

open class CompositeElement(_position: Offset = Offset.Zero, _size: Size = Size.Unspecified) : Element {
    override var attachedProperties by mutableStateOf(emptyMap<KClass<*>, Any?>())

    override var parent: Element? by mutableStateOf(null)
    override var position by mutableStateOf(_position)
    override var size by mutableStateOf(_size)
    override val minimumSize: Size
        get() = Size(children.maxOf { it.minimumSize.width }, children.maxOf { it.minimumSize.height })
    override var visible by mutableStateOf(true)
    override var children by mutableStateOf(emptyList<Element>())
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

inline fun <reified ContainerT : Any, PropertyT, reified ParentT : Element?> attached(
    containerProperty: KMutableProperty1<ContainerT, PropertyT>,
    recursive: Boolean = false,
    crossinline default: () -> PropertyT,
) = object : ReadWriteProperty<Element, PropertyT> {
    override fun getValue(thisRef: Element, property: KProperty<*>): PropertyT {
        var element: Element? = thisRef
        while (element != null) {
            val elementContainer = element.attachedProperties[ContainerT::class]
            val a = element.parent is ParentT
            val b = "${element.parent}"
            val c = "${ParentT::class}"
            if (element.parent is ParentT && elementContainer != null) {
                return containerProperty.call(elementContainer)
            }
            if (!recursive) {
                break
            }
            element = element.parent
        }
        return default()
    }

    override fun setValue(thisRef: Element, property: KProperty<*>, value: PropertyT) {
        if (thisRef.parent !is ParentT) {
            error("No parent of type ${ParentT::class} found for $this")
        }

        if (ContainerT::class !in thisRef.attachedProperties) {
            thisRef.attachedProperties += ContainerT::class to ContainerT::class.createInstance()
        }

        val container = thisRef.attachedProperties[ContainerT::class] as ContainerT
        containerProperty.set(container, value)
    }
}
