package com.bsuir.objviewer.core.assetsprovider

import com.bsuir.objviewer.core.extension.normalized
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import java.awt.image.BufferedImage

interface NormalProvider {
    fun get(x: Float, y: Float): D1Array<Double>
}

class ImageNormalProvider(image: BufferedImage) : NormalProvider {

    private val pixelProvider = ImagePixelProvider(image)
    private val maxByte = Byte.MAX_VALUE.toDouble()

    override fun get(x: Float, y: Float): D1Array<Double> {
            val argb = pixelProvider.get(x, y)
            val r = argb.shr(16).toByte()
            val g = argb.shr(8).toByte()
            val b = argb.toByte()
        return mk.ndarray(mk[r / maxByte, g / maxByte, b / maxByte])
    }
}
