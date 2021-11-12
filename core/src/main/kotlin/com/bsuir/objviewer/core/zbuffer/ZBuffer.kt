package com.bsuir.objviewer.core.zbuffer

import com.bsuir.objviewer.core.extension.packInts
import com.bsuir.objviewer.core.extension.unpackInt1
import com.bsuir.objviewer.core.extension.unpackInt2
import com.bsuir.objviewer.core.model.Color
import com.bsuir.objviewer.core.model.IntSize

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

private fun DepthAndColor(depth: UInt, color: Color) = DepthAndColor(packInts(depth.toInt(), color.packed))

typealias PointConsumer = (x: Int, y: Int, color: Color) -> Unit

class ZBuffer(
    private val maxWidth: Int,
    private val maxHeight: Int,
    private val blank: Color
) {

    private var widthRange = 0 until maxWidth
    private var heightRange = 0 until maxHeight

    private val items: Array<Array<DepthAndColor>> =
        Array(maxHeight) {
            Array(maxWidth) { DepthAndColor(UInt.MAX_VALUE, blank) }
        }

    operator fun get(i: Int): Array<DepthAndColor> {
        return items[i]
    }

    fun setSize(size: IntSize){
        if (size != IntSize(widthRange.last, heightRange.last)){
            if (size.width > maxWidth || size.height > maxHeight){
                widthRange = 0 until maxWidth
                heightRange = 0 until maxHeight
            } else {
                widthRange = 0 until size.width
                heightRange = 0 until size.height
            }
        }
    }

    fun invalidate() {
        for (y: Int in heightRange) {
            for (x: Int in widthRange) {
                items[y][x] = DepthAndColor(UInt.MAX_VALUE, blank)
            }
        }
    }

    fun addPoint(x: Int, y: Int, depth: UInt, color: Color) {
        if (x in widthRange && y in heightRange) {
            if (this[y][x].depth > depth) {
                this[y][x] = DepthAndColor(depth, color)
            }
        }
    }

    fun transferTo(consumer: PointConsumer) {
        for (y: Int in heightRange) {
            for (x: Int in widthRange) {
                consumer(x, y, items[y][x].color)
            }
        }
    }
}
