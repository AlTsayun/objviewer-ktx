package com.bsuir.objviewer.core.zbuffer

import com.bsuir.objviewer.core.extension.*
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
value class DepthAndColor internal constructor(val packed: Long) {
    val depth: Float
        get() = unpackFloat1(packed)

    val color: Color
        get() = Color(unpackInt2(packed))
}

private fun DepthAndColor(depth: Float, color: Color) = DepthAndColor(packFloatAndInt(depth, color.packed))

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
            Array(maxWidth) { DepthAndColor(Float.MAX_VALUE, blank) }
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
                items[y][x] = DepthAndColor(Float.MAX_VALUE, blank)
            }
        }
    }

    fun addPoint(x: Int, y: Int, depth: Float, color: Color) {
        if (x in widthRange && y in heightRange) {
            if (items[y][x].depth > depth) {
                items[y][x] = DepthAndColor(depth, color)
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
