
@file:Suppress("NOTHING_TO_INLINE")
package com.bsuir.objviewer.core.model

import com.bsuir.objviewer.core.extension.packInts
import com.bsuir.objviewer.core.extension.unpackInt1
import com.bsuir.objviewer.core.extension.unpackInt2

/**
 * Constructs an [IntSize] from width and height [Int] values.
 */
fun IntSize(width: Int, height: Int): IntSize = IntSize(packInts(width, height))

/**
 * A two-dimensional size class used for measuring in [Int] pixels.
 */
@JvmInline
value class IntSize internal constructor(private val packedValue: Long) {

    /**
     * The horizontal aspect of the size in [Int] pixels.
     */
    val width: Int
        get() = unpackInt1(packedValue)

    /**
     * The vertical aspect of the size in [Int] pixels.
     */
    val height: Int
        get() = unpackInt2(packedValue)

    inline operator fun component1(): Int = width

    inline operator fun component2(): Int = height

    /**
     * Returns an IntSize scaled by multiplying [width] and [height] by [other]
     */
    operator fun times(other: Int): IntSize =
        IntSize(width = width * other, height = height * other)

    /**
     * Returns an IntSize scaled by dividing [width] and [height] by [other]
     */
    operator fun div(other: Int): IntSize =
        IntSize(width = width / other, height = height / other)

    override fun toString(): String = "$width x $height"

    companion object {
        /**
         * IntSize with a zero (0) width and height.
         */
        val Zero = IntSize(0L)
    }
}

/**
 * Returns an [IntSize] with [size]'s [IntSize.width] and [IntSize.height]
 * multiplied by [this].
 */
operator fun Int.times(size: IntSize) = size * this
