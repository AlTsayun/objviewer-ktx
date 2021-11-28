package com.bsuir.objviewer.core.assetsprovider

import com.bsuir.objviewer.core.model.Color
import java.awt.image.BufferedImage

interface TextureProvider {
    fun get(x: Float, y: Float): Color
}

class PlainColorTextureProvider(val color: Color) : TextureProvider {
    override fun get(x: Float, y: Float): Color = color
}

class LighterTextureProvider(private val inner: TextureProvider, var lightness: Double) : TextureProvider{
    override fun get(x: Float, y: Float): Color = inner.get(x, y).multiplyLightness(lightness)
}

class ImageTextureProvider(image: BufferedImage) : TextureProvider {

    private val pixelProvider = ImagePixelProvider(image)

    @OptIn(ExperimentalStdlibApi::class)
    override fun get(x: Float, y: Float): Color {
        //rotating argb value to rgba
        return Color(pixelProvider.get(x, y).rotateLeft(8))
    }
}