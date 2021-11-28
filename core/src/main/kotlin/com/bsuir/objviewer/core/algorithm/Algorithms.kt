package com.bsuir.objviewer.core.algorithm

import com.bsuir.objviewer.core.extension.*
import com.bsuir.objviewer.core.model.*
import com.bsuir.objviewer.core.assetsprovider.TextureProvider
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.operations.plus
import org.jetbrains.kotlinx.multik.ndarray.operations.times
import kotlin.math.abs

typealias DepthPointConsumer = (x: Int, y: Int, depth: Float, color: Color) -> Unit

fun getCenter(points: List<D1Array<Double>>): D1Array<Double>{
    return mk.ndarray(listOf(
        points.map { it[0] }.average(),
        points.map { it[1] }.average(),
        points.map { it[2] }.average(),
    ))
}

//fun drawStrokeFace(face: ProcessedFace, consumer: DepthPointConsumer, color: Color){
//    face.vertices
//        .pairedInCycle()
//        .forEach { pair ->
//            drawLine(
//                pair.first.point.x,
//                pair.first.point.y,
//                0f,
//                0f,
//                1f,
//                pair.first.point.depth,
//                pair.second.point.x,
//                pair.second.point.y,
//                0f,
//                0f,
//                1f,
//                pair.second.point.depth,
//                PlainColorTextureProvider(color),
//                consumer
//            )
//        }
//}