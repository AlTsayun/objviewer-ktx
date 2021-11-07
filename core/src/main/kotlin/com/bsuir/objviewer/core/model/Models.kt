package com.bsuir.objviewer.core.model

import com.bsuir.objviewer.core.extension.*
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import java.util.*

data class FPoint2d(val x: Float, val y: Float)

data class DepthPoint2d(val x: Int, val y: Int, val depth: UInt)

data class Edge(
    val minX: Int,
    val minY: Int,
    val minDepth: UInt,
    val maxY: Int,
    val invDepthSlope: Float,
    val invXSlope: Float
)

fun Color(r: UByte, g: UByte, b: UByte, a: UByte): Color = Color(packUBytes(r, g, b, a))

@JvmInline
value class Color constructor(val packedValue: Int) {
    val r: UByte
        get() = unpackUByte1(packedValue)
    val g: UByte
        get() = unpackUByte2(packedValue)
    val b: UByte
        get() = unpackUByte3(packedValue)
    val a: UByte
        get() = unpackUByte4(packedValue)

    fun multiplyLightness(value: Double): Color{
        val r = r.timesNoOverflow(value)
        val g = g.timesNoOverflow(value)
        val b = b.timesNoOverflow(value)
        return Color(r, g, b, a)
    }
}

data class ProcessedWorldObject(
    val id: UUID,
    val faces: List<ProcessedFace>
)

data class ProcessedFace(val items: List<Item>, val color: Color) {
    data class Item(val point: DepthPoint2d)
}

data class VertexProjections(
    val model: NDArray<Double, D1>,
    val view: NDArray<Double, D1>,
    val projection: NDArray<Double, D1>,
    val viewport: NDArray<Double, D1>,
    val point: FPoint2d
)