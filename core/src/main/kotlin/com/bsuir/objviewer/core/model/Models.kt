package com.bsuir.objviewer.core.model

import com.bsuir.objviewer.core.extension.*
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import java.util.*
import kotlin.random.Random

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
value class Color constructor(val packed: Int) {
    val r: UByte
        get() = unpackUByte1(packed)
    val g: UByte
        get() = unpackUByte2(packed)
    val b: UByte
        get() = unpackUByte3(packed)
    val a: UByte
        get() = unpackUByte4(packed)

    fun multiplyLightness(value: Double): Color{
        val r = r.timesNoOverflow(value)
        val g = g.timesNoOverflow(value)
        val b = b.timesNoOverflow(value)
        return Color(r, g, b, a)
    }

    companion object {
        val WHITE = Color(UByte.MAX_VALUE, UByte.MAX_VALUE, UByte.MAX_VALUE, UByte.MAX_VALUE)
        val RED = Color(UByte.MAX_VALUE, 0u, 0u, UByte.MAX_VALUE)
        val GREEN = Color(0u, UByte.MAX_VALUE, 0u, UByte.MAX_VALUE)
        val BLUE = Color(0u, 0u, UByte.MAX_VALUE, UByte.MAX_VALUE)
        val BLACK = Color(0u, 0u, 0u, UByte.MAX_VALUE)

        val GRAY = Color(
            (UByte.MAX_VALUE / 2u).toUByte(),
            (UByte.MAX_VALUE / 2u).toUByte(),
            (UByte.MAX_VALUE / 2u).toUByte(),
            (UByte.MAX_VALUE / 2u).toUByte()
        )

        val RANDOM: Color
        get() {
            val value = Random.nextInt()
            return Color(value and 0xFF)
        }
    }
}

data class ProcessedWorldObject(
    val id: UUID,
    val faces: List<ProcessedFace>
)

data class ProcessedFace(val items: List<Item>, val color: Color) {
    data class Item(val point: DepthPoint2d)
}

class VertexesProjections(size: Int){
    val model: MutableList<D1Array<Double>> = ArrayList(size)
    val world: MutableList<D1Array<Double>> = ArrayList(size)
    val view: MutableList<D1Array<Double>> = ArrayList(size)
    val projection: MutableList<D1Array<Double>> = ArrayList(size)
    val viewport: MutableList<D1Array<Double>> = ArrayList(size)
    val screen: MutableList<DepthPoint2d> = ArrayList(size)

    fun add(
        modelV: NDArray<Double, D1>,
        worldV: NDArray<Double, D1>,
        viewV: NDArray<Double, D1>,
        projectionV: NDArray<Double, D1>,
        viewportV: NDArray<Double, D1>,
        pointV: DepthPoint2d
    ){
        model.add(modelV)
        world.add(worldV)
        view.add(viewV)
        projection.add(projectionV)
        viewport.add(viewportV)
        screen.add(pointV)
    }
}