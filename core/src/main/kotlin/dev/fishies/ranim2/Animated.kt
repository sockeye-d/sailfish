package dev.fishies.ranim2

interface Animated {
    val isFinished: Boolean

    /**
     * Runs one tick of the animation.
     * @return True if the animation is finished
     */
    fun tick()
}
