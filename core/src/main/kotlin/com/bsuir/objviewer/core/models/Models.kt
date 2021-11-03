package com.bsuir.objviewer.core.models

import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import java.util.*

data class FPoint2d(val x: Float, val y: Float)

data class DepthPoint2d(val x: Int, val y: Int, val depth: UInt)

data class Edge(val minX: Int, val minY: Int, val minDepth: UInt, val maxY: Int, val invDepthSlope: Float, val invSlope: Float)

//todo: rewrite Color data class
data class Color(val value: Int)

data class ProcessedWorldObject(
    val id: UUID,
    val faces: List<ProcessedFace>
)

data class ProcessedFace(val items: List<Item>) {
    data class Item(val point: DepthPoint2d)
}

data class VertexProjections(
    val model: NDArray<Double, D1>,
    val view: NDArray<Double, D1>,
    val projection: NDArray<Double, D1>,
    val viewport: NDArray<Double, D1>,
    val point: FPoint2d
)