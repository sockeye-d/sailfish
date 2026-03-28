package animations.colorPicker

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import dev.fishies.sailfish.*
import dev.fishies.sailfish.containers.*
import dev.fishies.sailfish.containers.Anchor
import dev.fishies.sailfish.elements.rectangle
import dev.fishies.sailfish.elements.text
import dev.fishies.sailfish.theming.theme
import dev.fishies.sailfish.tweener.*
import kotlin.reflect.KProperty1

private val loader = object {}::class.java.classLoader

// val catppuccinMocha = loadJson<Theme>("files/catppuccin-mocha.json")

class ColorPickerState {
    var color by mutableStateOf(Color.Red)
    var boxSize by mutableStateOf(Size(100f, 100f))
}

fun CompositeElement.channelBar(
    state: ColorPickerState,
    label: String,
    color: Color,
    channel: KProperty1<Color, Float>,
) = linear(Axis.X) {
    separation = 10F
    box {
        fraction = 1.0f
        rectangle(Size(0.0f, 15.0f), color)() {
            anchor = Anchor.fill
            respectsPadding = false
        }
        rectangle(Size(2.0f, 0.0f), theme.onBackground)() {
            ::anchor.bind {
                val color = state.color.convert(ColorSpaces.LinearSrgb)
                Anchor(
                    AxisAnchor.Absolute(channel(color)),
                    AxisAnchor.Fill,
                )
            }
        }
    }
    text(label)
}

private fun CompositeElement.colorPicker(state: ColorPickerState) = panel(radius = 8.0f) {
    padding = Padding(all = 8.0f)
    linear(Axis.Y) {
        separation = 10F
        rectangle(Size(200f, 100f))() {
            ::customMinimumSize.bind { state.boxSize }
            ::color.bind { state.color }
        }

        channelBar(state, "R", Color.Red, Color::red)
        channelBar(state, "G", Color.Green, Color::green)
        channelBar(state, "B", Color.Blue, Color::blue)
    }
}

@AnimationProvider
fun colorPickerAnimation() = animation {
    // theme = catppuccinMocha
    val state = ColorPickerState()

    val picker = colorPicker(state)
    picker.position = Offset(250f, 150f)

    yield("event")
    yield("event2")
    yield("event3")
    yield("event4")
    // println("hi")

    val colors = listOf(theme.primary, theme.secondary, theme.error)

    for (color in colors) {
        yield(state::color.tween(to = color, length = 25, tweener = quadratic(InOut), colorSpace = ColorSpaces.Oklab))
    }
}

@AnimationProvider
fun showHi() = animation {
    val hi = text("i am NO LONGER getting internal kotlinc errors they DON'T look scary", position = Offset(50f, 50f))
    for (i in 1..10) {
        yield(hi::position.tween(to = Offset(100f, 50f), length = 30, tweener = cubic(InOut)))
        yield(hi::position.tween(to = Offset(50f, 50f), length = 30, tweener = cubic(InOut)))
    }
}

@AnimationProvider
fun showHi2() = animation {
    val hi = text("Please work ", position = Offset(50f, 50f), color = theme.onBackground)
    yield(hi::position.tween(to = Offset(100f, 50f), length = 100, tweener = cubic(InOut)))
    yield(hi::position.tween(to = Offset(50f, 50f), length = 100, tweener = cubic(InOut)))
}

@AnimationProvider
fun showHi3() = animation {
    val hi = rectangle(Size(50f, 50f), position = Offset(50f, 50f), color = theme.onBackground)
    yield(hi::position.tween(to = Offset(100f, 50f), length = 100, tweener = cubic(InOut)))
}
