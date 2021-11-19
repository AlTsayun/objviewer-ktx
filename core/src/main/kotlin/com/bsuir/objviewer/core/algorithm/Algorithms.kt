package com.bsuir.objviewer.core.algorithm

import com.bsuir.objviewer.core.extension.*
import com.bsuir.objviewer.core.model.*
import com.bsuir.objviewer.core.textureprovider.PlainColorTextureProvider
import com.bsuir.objviewer.core.textureprovider.TextureProvider
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import kotlin.math.abs

typealias DepthPointConsumer = (x: Int, y: Int, depth: Float, color: Color) -> Unit

fun getCenter(points: List<D1Array<Double>>): D1Array<Double>{
    return mk.ndarray(listOf(
        points.map { it[0] }.average(),
        points.map { it[1] }.average(),
        points.map { it[2] }.average(),
    ))
}

fun drawLine(
    x1: Int,
    y1: Int,
    xt1: Float,
    yt1: Float,
    lightness1: Float,
    depth1: Float,
    x2: Int,
    y2: Int,
    xt2: Float,
    yt2: Float,
    lightness2: Float,
    depth2: Float,
    textureProvider: TextureProvider,
    consumer: DepthPointConsumer
) {
    dda(
        x1.toFloat(),
        y1.toFloat(),
        xt1,
        yt1,
        lightness1,
        depth1,
        x2.toFloat(),
        y2.toFloat(),
        xt2,
        yt2,
        lightness2,
        depth2,
        textureProvider,
        consumer
    )
}


private fun dda(
    x1: Float,
    y1: Float,
    xt1: Float,
    yt1: Float,
    lightness1: Float,
    depth1: Float,
    x2: Float,
    y2: Float,
    xt2: Float,
    yt2: Float,
    lightness2: Float,
    depth2: Float,
    textureProvider: TextureProvider,
    consumer: DepthPointConsumer
) {
    val deltaX = x2 - x1
    val deltaXt = xt2 - xt1
    val deltaY = y2 - y1
    val deltaYt = yt2 - yt1
    val deltaDepth = depth2 - depth1
    val deltaLightness = lightness2 - lightness1

    val delta = if (abs(deltaX) > abs(deltaY)) {
        abs(deltaX)
    } else {
        abs(deltaY)
    } + 1

    val dirX = deltaX / delta
    val dirXt = deltaXt / delta
    val dirY = deltaY / delta
    val dirYt = deltaYt / delta
    val dirDepth = deltaDepth / delta
    val dirLightness = deltaLightness / delta

    var x: Float = x1
    var y: Float = y1
    var xt = xt1
    var yt = yt1
    var depth = depth1
    var lightness = lightness1

    for (i in 0..delta.toInt()) {
        consumer(x.toInt(), y.toInt(), depth, textureProvider.get(xt, yt).multiplyLightness(lightness))
        x += dirX
        y += dirY
        xt += dirXt
        yt += dirYt
        depth += dirDepth
        lightness += dirLightness
    }
}

fun drawFillFace(
    face: ProcessedFace,
    textureProvider: TextureProvider,
    consumer: DepthPointConsumer
) {
    val allEdges = face.vertices
        .pairedInCycle()
        .map { pair ->
            val i1 = pair.first
            val i2 = pair.second
            if (i1.point.y < i2.point.y) {
                Edge(i1, i2)
            } else {
                Edge(i2, i1)
            }
        }

    val yComparator = compareBy<Edge> { it.lowerPoint.y }
    val xComparator = compareBy<Edge> { it.lowerPoint.x }
    val globalEdges = allEdges
//        .filter { it.invXSlope != 0f }
        .sortedWith(yComparator.then(xComparator))

    if (globalEdges.isNotEmpty()) {
        for (scanLine: Int in globalEdges.first().lowerPoint.y until globalEdges.last().higherY){
            globalEdges
                .filter { (it.lowerPoint.y <= scanLine) && (it.higherY > scanLine) }
                .chunked(2)
                .forEach {
                    val fromX = getCurrentX(it[0], scanLine)
                    val fromDepth = getCurrentDepth(it[0], scanLine)
                    val fromTexture = getCurrentTexture(it[0], scanLine)
                    val fromLightness = getCurrentLightness(it[0], scanLine)
                    val toX = getCurrentX(it[1], scanLine)
                    val toDepth = getCurrentDepth(it[1], scanLine)
                    val toTexture = getCurrentTexture(it[1], scanLine)
                    val toLightness = getCurrentLightness(it[1], scanLine)
                    drawLine(
                        fromX,
                        scanLine,
                        fromTexture.x,
                        fromTexture.y,
                        fromLightness,
                        fromDepth,
                        toX,
                        scanLine,
                        toTexture.x,
                        toTexture.y,
                        toLightness,
                        toDepth,
                        textureProvider,
                        consumer
                    )
                }
        }
    }

}

private fun getCurrentX(edge: Edge, y: Int): Int{
    return edge.lowerPoint.x + ((y - edge.lowerPoint.y) * edge.invXSlope).toInt()
}

private fun getCurrentDepth(edge: Edge, y: Int): Float{
    return edge.lowerPoint.depth + ((y - edge.lowerPoint.y) * edge.invDepthSlope)
}

private fun getCurrentLightness(edge: Edge, y: Int): Float{
    return edge.lowerLightness + ((y - edge.lowerPoint.y) * edge.invLightnessSlope)
}

private fun getCurrentTexture(edge: Edge, y: Int): FPoint2d{
    return FPoint2d(
        edge.lowerTexture.x + ((y - edge.lowerPoint.y) * edge.invTextureXSlope),
        edge.lowerTexture.y + ((y - edge.lowerPoint.y) * edge.invTextureYSlope)
    )

}

fun drawStrokeFace(face: ProcessedFace, consumer: DepthPointConsumer, color: Color){
    face.vertices
        .pairedInCycle()
        .forEach { pair ->
            drawLine(
                pair.first.point.x,
                pair.first.point.y,
                0f,
                0f,
                1f,
                pair.first.point.depth,
                pair.second.point.x,
                pair.second.point.y,
                0f,
                0f,
                1f,
                pair.second.point.depth,
                PlainColorTextureProvider(color),
                consumer
            )
        }
}