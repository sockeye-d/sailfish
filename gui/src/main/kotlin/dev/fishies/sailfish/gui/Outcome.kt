package dev.fishies.sailfish.gui

import androidx.compose.runtime.Immutable

@Immutable
sealed class Outcome<out T> {
    @Immutable
    data object Progress : Outcome<Nothing>()

    @Immutable
    data class Success<T>(val data: T) : Outcome<T>()

    @Immutable
    data class Error(val exception: Exception) : Outcome<Nothing>()
}

val <T> Outcome<T>.dataOrNull
    get() = when (this) {
        is Outcome.Success -> data
        else -> null
    }
