package animations.colorPicker

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import dev.fishies.ranim2.*
import dev.fishies.ranim2.containers.*
import dev.fishies.ranim2.containers.Anchor
import dev.fishies.ranim2.elements.rectangle
import dev.fishies.ranim2.elements.text
import dev.fishies.ranim2.theming.Theme
import dev.fishies.ranim2.theming.theme
import dev.fishies.ranim2.tweener.*
import dev.fishies.ranim2.util.loadJson
import kotlin.reflect.KProperty1

private val loader = object{}::class.java.classLoader

val catppuccinMocha = loadJson<Theme>("files/catppuccin-mocha.json")

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
        rectangle(Size(100f, 100f))() {
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
    theme = catppuccinMocha
    val state = ColorPickerState()

    val picker = colorPicker(state)
    picker.position = Offset(150f, 150f)

    while (true) {
        yield(
            state::color.tween(
                to = theme.primary, length = 300, tweener = quadratic(InOut), colorSpace = ColorSpaces.Oklab
            )
        )
        yield(
            state::color.tween(
                to = theme.secondary, length = 300, tweener = quadratic(InOut), colorSpace = ColorSpaces.Oklab
            )
        )
        yield(
            state::color.tween(
                to = theme.error, length = 300, tweener = quadratic(InOut), colorSpace = ColorSpaces.Oklab
            )
        )
    }
}

@AnimationProvider
fun showHi() = animation {
    val hi = text("This is some other text!", position = Offset(50f, 50f))
    while (true) {
        yield(hi::position.tween(to = Offset(100f, 50f), length = 100, tweener = cubic(InOut)))
        yield(hi::position.tween(to = Offset(50f, 50f), length = 100, tweener = cubic(InOut)))
    }
}

// @AnimationProvider
// fun showHi2() = animation {
//     val hi = text("This is some other text!", position = Offset(50f, 50f))
//     while (true) {
//         yield(hi::position.tween(to = Offset(100f, 50f), length = 100, tweener = cubic(InOut)))
//         yield(hi::position.tween(to = Offset(50f, 50f), length = 100, tweener = cubic(InOut)))
//     }
// }
