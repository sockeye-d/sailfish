package dev.fishies.ranim2.core

import androidx.compose.runtime.snapshots.Snapshot
import kotlin.reflect.KMutableProperty

fun <T> KMutableProperty<T>.bind(calculation: () -> T) {
    val readSet = mutableSetOf<Any>()
    val readObserver: (Any) -> Unit = {
        readSet.add(it)
    }

    var lastValue = getter.call()
    var disposeObserver: (() -> Unit)? = null

    fun takeSnapshot() {
        readSet.clear()
        val value = Snapshot.takeSnapshot(readObserver).run {
            try {
                enter(calculation)
            } finally {
                dispose()
            }
        }
        if (value != lastValue) {
            lastValue = value
            setter.call(value)
        }
    }

    val observer = Snapshot.registerApplyObserver { changed, _ ->
        if (readSet.any { it in changed }) {
            takeSnapshot()
        }
    }

    disposeObserver = observer::dispose
    // TODO: figure out when to actually call `disposeObserver?.invoke()`

    takeSnapshot()
}
