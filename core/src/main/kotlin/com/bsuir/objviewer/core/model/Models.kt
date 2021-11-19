package com.bsuir.objviewer.core.model

import com.bsuir.objviewer.core.extension.*
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import java.util.*
import kotlin.random.Random

fun FPoint2d(x: Float, y: Float) = FPoint2d(packFloats(x, y))

@JvmInline
value class FPoint2d constructor(val packed: Long) {
    val x: Float
        get() = unpackFloat1(packed)
    val y: Float
        get() = unpackFloat2(packed)
}

data class DepthPoint2d(val x: Int, val y: Int, val depth: Float)

data class Edge(
    private val lowerVertex: ProcessedFace.Vertex,
    private val higherVertex: ProcessedFace.Vertex,
) {
    val lowerPoint = lowerVertex.point
    private val higherPoint = higherVertex.point

    val invXSlope: Float = (higherPoint.x - lowerPoint.x) / (higherPoint.y - lowerPoint.y).toFloat()

    val lowerTexture = lowerVertex.texture

    val invTextureXSlope = (higherVertex.texture.x - lowerTexture.x) / (higherPoint.y - lowerPoint.y)
    val invTextureYSlope = (higherVertex.texture.y - lowerTexture.y) / (higherPoint.y - lowerPoint.y)

    val lowerLightness = lowerVertex.lightness
    val invLightnessSlope = (higherVertex.lightness - lowerVertex.lightness) / (higherPoint.y - lowerPoint.y)

    val higherY: Int = higherPoint.y
    val invDepthSlope: Float = (higherPoint.depth - lowerPoint.depth) / (higherPoint.y - lowerPoint.y).toFloat()
}

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

    /**
     * Pass 1 to leave unchanged
     */
    fun multiplyLightness(value: Float): Color{
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
            (UByte.MAX_VALUE)
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

data class ProcessedFace(val vertices: List<Vertex>) {
    data class Vertex(val point: DepthPoint2d, val lightness: Float, val texture: FPoint2d)
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