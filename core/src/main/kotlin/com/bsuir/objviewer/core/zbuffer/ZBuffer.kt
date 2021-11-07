package com.bsuir.objviewer.core.zbuffer

import com.bsuir.objviewer.core.extension.packInts
import com.bsuir.objviewer.core.extension.unpackInt1
import com.bsuir.objviewer.core.extension.unpackInt2
import com.bsuir.objviewer.core.model.Color

@JvmInline
value class Coordinates internal constructor(private val packedValue: Long) {
    val x: Int
        get() = unpackInt1(packedValue)

    val y: Int
        get() = unpackInt2(packedValue)
}

private fun Coordinates(x: Int, y: Int) = Coordinates(packInts(x, y))

@JvmInline
value class DepthAndColor internal constructor(private val packedValue: Long) {
    val depth: UInt
        get() = unpackInt1(packedValue).toUInt()

    val color: Color
        get() = Color(unpackInt2(packedValue))
}

private fun DepthAndColor(depth: UInt, color: Color) = DepthAndColor(packInts(depth.toInt(), color.packedValue))

typealias PointConsumer = (x: Int, y: Int, color: Color) -> Unit

class ZBuffer(private val maxWidth: Int, private val maxHeight: Int, private val blank: Color) {
    private val items: Array<Array<DepthAndColor>> =
        Array(maxHeight) {
            Array(maxWidth) { DepthAndColor(UInt.MAX_VALUE, blank) }
        }

    var width: Int = maxWidth
    var height: Int = maxHeight

    operator fun get(i: Int): Array<DepthAndColor> {
        return items[i]
    }

    fun invalidate() {
        for (y: Int in 0 until height) {
            for (x: Int in 0 until width) {
                items[y][x] = DepthAndColor(UInt.MAX_VALUE, blank)
            }
        }
    }

    fun addPoint(x: Int, y: Int, depth: UInt, color: Color) {
        if (this[y][x].depth > depth) {
            this[y][x] = DepthAndColor(depth, color)
        }
    }

    fun transferTo(consumer: PointConsumer) {
        for (y: Int in 0 until height) {
            for (x: Int in 0 until width) {
                consumer(x, y, items[y][x].color)
            }
        }
    }
}