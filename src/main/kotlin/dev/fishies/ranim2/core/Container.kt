package dev.fishies.ranim2.core

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.full.createInstance

abstract class Container<ContainerT : Any>(protected val makeContainer: () -> ContainerT) : CompositeElement() {
    var attachedPropertyContainers = mutableStateMapOf<Element, ContainerT>()

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
    }

    override fun removeChild(element: Element) {
        super.removeChild(element)
        compactPropertyContainers()
    }

    override fun removeChild(elements: List<Element>) {
        super.removeChild(elements)
        compactPropertyContainers()
    }

    fun compactPropertyContainers() {
        for ((element) in attachedPropertyContainers) {
            if (element.parent == null) attachedPropertyContainers -= element
        }
    }

    fun <PropertyT> parentAttached(
        containerProperty: KMutableProperty1<ContainerT, PropertyT>,
        default: () -> PropertyT,
    ) = object : ReadWriteProperty<Element, PropertyT> {
        /**
         * To get a value, get the container, then access the property provided by [containerProperty].
         */
        override fun getValue(thisRef: Element, property: KProperty<*>) =
            attachedPropertyContainers[thisRef]?.let { containerProperty.get(it) } ?: default()

        override fun setValue(thisRef: Element, property: KProperty<*>, value: PropertyT) {
            val container = attachedPropertyContainers[thisRef] ?: makeContainer()
            containerProperty.set(container, value)
            attachedPropertyContainers += thisRef to container
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun Element.fitInRect(rect: Rect) {
        position = rect.topLeft
        size = rect.size
    }
}

inline fun <reified ContainerT : Any, PropertyT> parentAttached(
    containerProperty: KMutableProperty1<ContainerT, PropertyT>,
    crossinline default: () -> PropertyT,
) = object : ReadWriteProperty<Element, PropertyT> {
    private fun Element.getParentContainer(): Container<ContainerT> {
        return parent as? Container<ContainerT> ?: error("No parent found for $this")
    }

    /**
     * To get a value, get the container, then access the property provided by [containerProperty].
     */
    override fun getValue(thisRef: Element, property: KProperty<*>): PropertyT {
        return thisRef.getParentContainer().attachedPropertyContainers[thisRef]?.let { containerProperty.get(it as ContainerT) }
            ?: default()
    }

    override fun setValue(thisRef: Element, property: KProperty<*>, value: PropertyT) {
        val eContainer = thisRef.getParentContainer()
        val container = eContainer.attachedPropertyContainers[thisRef] ?: ContainerT::class.createInstance()
        containerProperty.set(container, value)
        eContainer.attachedPropertyContainers[thisRef] = container
    }
}
