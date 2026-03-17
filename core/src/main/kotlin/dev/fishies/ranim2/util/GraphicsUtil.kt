package dev.fishies.ranim2.util

import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.toAwtImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.imageio.ImageIO

suspend fun GraphicsLayer.saveImage(file: File = File("/home/fish/Downloads/file.png")) = withContext(Dispatchers.IO) {
    ImageIO.write(
        toImageBitmap().toAwtImage(),
        file.extension,
        file,
    )
}
