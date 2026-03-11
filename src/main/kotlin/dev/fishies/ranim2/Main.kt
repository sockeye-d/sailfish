package dev.fishies.ranim2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withCompositionLocal
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.SkiaGraphicsContext
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.fishies.ranim2.containers.Anchor
import dev.fishies.ranim2.containers.BoxContainer
import dev.fishies.ranim2.containers.anchor
import dev.fishies.ranim2.containers.boxContainer
import dev.fishies.ranim2.containers.fraction
import dev.fishies.ranim2.containers.linearContainer
import dev.fishies.ranim2.core.*
import dev.fishies.ranim2.elements.ShapeElement
import dev.fishies.ranim2.elements.layer
import dev.fishies.ranim2.elements.makePainter
import dev.fishies.ranim2.elements.makeRectangle
import dev.fishies.ranim2.elements.makeText
import dev.fishies.ranim2.ranim2.generated.resources.Res
import dev.fishies.ranim2.theming.Theme
import dev.fishies.ranim2.theming.defaultTheme
import dev.fishies.ranim2.theming.theme
import dev.fishies.ranim2.theming.toComposeColors
import dev.fishies.ranim2.tweener.InOut
import dev.fishies.ranim2.tweener.Out
import dev.fishies.ranim2.tweener.cubic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import javax.imageio.ImageIO

//private fun svgPainter(resource: DrawableResource): Painter {
//    resource
//val resourceReader = LocalResourceReader.currentOrPreview
//val density = LocalDensity.current
//val svgPainter by rememberResourceState(resource, resourceReader, density, { emptySvgPainter }) { env ->
//    val path = resource.getResourceItemByEnvironment(env).path
//    val cached = loadImage(path, path, resourceReader) {
//        ImageCache.Svg(it.toSvgElement().toSvgPainter(density))
//    } as ImageCache.Svg
//    cached.painter
//}
//return svgPainter
//}

val catppuccinMocha = loadJson<Theme>(Res.getUri("files/catppuccin-mocha.json"))

fun bug() = animation {
    val circle = makePainter(loadImage(Res.getUri("drawable/bug.png")))
    circle.size *= 0.1f

    repeat(10) {
        yield(circle::position.tween(to = Offset(10f, it * 10f), length = 50, tweener = cubic(InOut)))
        yield(frames = 20)
    }
}

@OptIn(ExperimentalTextApi::class)
val anim = animation {
    theme = catppuccinMocha
    var inner: Element
    Container.drawContainerOutlines = true
    var container: BoxContainer

    layer {
        alpha = 0.5f
        renderEffect = BlurEffect(10.0f, 10.0f, TileMode.Decal)
        container = boxContainer {
            inner = linearContainer(separation = 3.0f) {
                anchor = Anchor.center
                makeRectangle(Size(20f, 20f), theme.primary, radius = 5.0f)() {
                    fraction = 1.0f
                }
                boxContainer {
                    fraction = 1.0f
                    makeRectangle(Size(20f, 20f), theme.secondary, radius = 5.0f)
                    makeText("This is some text!", color = theme.onSecondary)() {
                        alignment = Center
                        anchor = Anchor.Wide.m
                    }
                }
                makeRectangle(Size(20f, 20f), theme.primaryVariant, radius = 5.0f)() {
                    fraction = 1.0f
                }
            }
        }
    }

    yield(container::size.tween(to = Size(500f, 120f), length = 200, tweener = cubic(Out)))
    yield(bug())

    yield(
        animation {
            while (true) {
                fun animateInnerTo(anchor: Anchor, length: Int = 100) =
                    inner::anchor.tween(to = anchor, length = length, tweener = cubic(InOut))

                yield(animateInnerTo(Anchor.Shrink.tl, 200))
                yield(animateInnerTo(Anchor.Shrink.tm))
                yield(animateInnerTo(Anchor.Shrink.tr))
                yield(animateInnerTo(Anchor.Shrink.ml))
                yield(animateInnerTo(Anchor.Shrink.mm))
                yield(animateInnerTo(Anchor.Shrink.mr))
                yield(animateInnerTo(Anchor.Shrink.bl))
                yield(animateInnerTo(Anchor.Shrink.bm))
                yield(animateInnerTo(Anchor.Shrink.br))
                yield(animateInnerTo(Anchor.fill, 200))
            }
        },
        //animation {
        //    while (true) {
        //        yield(inner::color.tween(to = theme.secondary, length = 50, tweener = cubic(Out)))
        //        yield(inner::color.tween(to = theme.primary, length = 50, tweener = cubic(Out)))
        //    }
        //}
    )
}

@OptIn(InternalComposeUiApi::class)
fun main() = application {
    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    val layer: GraphicsLayer = withCompositionLocal(LocalGraphicsContext provides SkiaGraphicsContext()) {
        val graphicsLayer = rememberGraphicsLayer()
        graphicsLayer.record(Density(3f), LayoutDirection.Ltr, layerSize) {
            anim.runLayoutPass()
            with(anim) { draw() }
        }
        graphicsLayer
    }

    MaterialTheme(colors = defaultTheme.toComposeColors()) {
        Window(onCloseRequest = ::exitApplication, title = "My Desktop App") {
            LaunchedEffect(Unit) {
                while (!anim.isFinished) {
                    withFrameMillis {
                        anim.tick()
                    }
                }
            }

            Box(Modifier.background(MaterialTheme.colors.background).fillMaxSize().onSizeChanged { layerSize = it }) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.onBackground) {
                    Column {
                        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            drawLayer(layer)
                        }
                        Row(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button({ runBlocking { layer.saveImage() } }) {
                                Text("Save image")
                            }

                            Button({ println(anim.treeString()) }) {
                                Text("Dump scene tree")
                            }

                            Row {
                                Text("Debug layout bounds", Modifier.align(Alignment.CenterVertically))
                                with(Container) {
                                    Switch(drawContainerOutlines, { drawContainerOutlines = it })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun GraphicsLayer.saveImage(file: File = File("/home/fish/Downloads/file.png")) =
    withContext(Dispatchers.IO) {
        ImageIO.write(
            toImageBitmap().toAwtImage(),
            file.extension,
            file,
        )
    }
