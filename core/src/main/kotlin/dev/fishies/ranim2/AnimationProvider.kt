package dev.fishies.ranim2

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class AnimationProvider(val framerate: Int = 60)
