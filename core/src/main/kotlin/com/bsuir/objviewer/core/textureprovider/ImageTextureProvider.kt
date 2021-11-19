package com.bsuir.objviewer.core.textureprovider

import com.bsuir.objviewer.core.model.Color
import java.awt.image.BufferedImage


class ImageTextureProvider(val image: BufferedImage) : TextureProvider {

    private val maxWidthIndex = image.width - 1
    private val maxHeightIndex = image.height - 1

    override fun get(x: Float, y: Float): Color {
        val argb = image.getRGB((x * maxWidthIndex).toInt(), ((1 - y) * maxHeightIndex).toInt())
        val a = argb.shr(24) and 0xFF
        val rgba = argb.shl(8) or a
        return Color(rgba)
    }
}