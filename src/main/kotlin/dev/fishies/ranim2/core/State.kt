package dev.fishies.ranim2.core

import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.reflect.KMutableProperty

private object NoValue

/**
 * Binds `this` to the result [calculation]. Whenever any of [calculation]'s dependent variables change, the value will
 * be recomputed and assigned to `this`.
 */
fun <T> KMutableProperty<T>.bind(calculation: () -> T) {
    val readSet = mutableSetOf<Any>()
    val readObserver: (Any) -> Unit = {
        readSet.add(it)
    }

    var lastValue: Any? = NoValue

    fun takeSnapshot() {
        readSet.clear()
        val value = Snapshot.takeSnapshot(readObserver).run {
            try {
                enter(calculation)
            } finally {
                dispose()
            }
        }
        if (value != lastValue || lastValue == NoValue) {
            lastValue = value
            setter.call(value)
        }
    }

    val observer = Snapshot.registerApplyObserver { changed, _ ->
        if (readSet.any { it in changed }) {
            takeSnapshot()
        }
    }
    BindingManager.disposeAndRegister(this, observer)

    takeSnapshot()
}

/**
 * Unbinds `this` from any bound computations. Doesn't do anything if no computation is already bound `this`.
 */
fun <T> KMutableProperty<T>.unbind() = BindingManager.dispose(this)

private object BindingManager {
    private val bindings = mutableMapOf<KMutableProperty<*>, ObserverHandle>()

    fun disposeAndRegister(property: KMutableProperty<*>, observer: ObserverHandle) {
        dispose(property)
        bindings[property] = observer
    }

    fun dispose(property: KMutableProperty<*>) {
        bindings.remove(property)?.dispose()
    }
}
