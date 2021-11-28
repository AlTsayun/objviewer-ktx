package com.bsuir.objviewer.core.assetsprovider

import java.awt.image.BufferedImage

interface SpecularProvider {
    fun get(x: Float, y: Float): Float
}

class ImageSpecularProvider(image: BufferedImage) : SpecularProvider {

    private val pixelProvider = ImagePixelProvider(image)

    private val maxByte = UByte.MAX_VALUE.toFloat()

    override fun get(x: Float, y: Float): Float {
        val b = pixelProvider.get(x, y).and(0xFF)
        return b / maxByte
    }
}

class ConstantSpecularProvider(val value: Float) : SpecularProvider {
    override fun get(x: Float, y: Float): Float = value
}