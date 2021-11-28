package com.bsuir.objviewer.core.assetsprovider

import java.awt.image.BufferedImage

interface PixelProvider {
    fun get(x: Float, y: Float): Int
}

class ImagePixelProvider(private val image: BufferedImage): PixelProvider {

    private val maxWidthIndex = image.width - 1
    private val maxHeightIndex = image.height - 1

    override fun get(x: Float, y: Float): Int =
        image.getRGB((x * maxWidthIndex).toInt(), ((1 - y) * maxHeightIndex).toInt())
}