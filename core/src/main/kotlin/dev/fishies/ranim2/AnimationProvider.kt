package dev.fishies.ranim2

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class AnimationProvider(val length: Int = Int.MAX_VALUE)
