package com.bsuir.objviewer.core.renderer

import com.bsuir.objviewer.core.assetsprovider.TextureProvider
import com.bsuir.objviewer.core.extension.cross
import com.bsuir.objviewer.core.extension.normalized
import com.bsuir.objviewer.core.extension.pairedInCycle
import com.bsuir.objviewer.core.model.Color
import com.bsuir.objviewer.core.model.Edge
import com.bsuir.objviewer.core.model.LightSource
import com.bsuir.objviewer.core.model.ProcessedFace
import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.operations.minus
import kotlin.math.abs

class LambertRenderer(
    private val depthPointConsumer: DepthPointConsumer,
    var color: Color
) : Renderer {

    private lateinit var lightSources: List<LightSource>

    fun setupWorldEnvironment(lightSources: List<LightSource>) {
        this.lightSources = lightSources
    }

    private fun dda(
        x1: Float,
        y1: Float,
        depth1: Float,
        x2: Float,
        y2: Float,
        depth2: Float,
        color: Color,
        consumer: DepthPointConsumer
    ) {
        val deltaX = x2 - x1
        val deltaY = y2 - y1
        val deltaDepth = depth2 - depth1

        val delta = if (abs(deltaX) > abs(deltaY)) {
            abs(deltaX)
        } else {
            abs(deltaY)
        } + 1

        val dirX = deltaX / delta
        val dirY = deltaY / delta
        val dirDepth = deltaDepth / delta

        var x: Float = x1
        var y: Float = y1
        var depth = depth1

        for (i in 0..delta.toInt()) {
            consumer(x.toInt(), y.toInt(), depth, color)
            x += dirX
            y += dirY
            depth += dirDepth
        }
    }

    fun drawFillFace(
        face: ProcessedFace,
        color: Color,
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
                        val toX = getCurrentX(it[1], scanLine)
                        val toDepth = getCurrentDepth(it[1], scanLine)
                        dda(
                            fromX.toFloat(),
                            scanLine.toFloat(),
                            fromDepth,
                            toX.toFloat(),
                            scanLine.toFloat(),
                            toDepth,
                            color,
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


    override fun consumeFace(face: ProcessedFace) {

        val faceNormal = getNormal(listOf(face.vertices[0].world, face.vertices[1].world, face.vertices[2].world))

        val invLightVectors = lightSources.map { light ->
            (face.vertices[0].world - light.coords).normalized()
        }

        val lightness = invLightVectors.map { (1 + (it dot faceNormal)) / 2 }.average()
        drawFillFace(face, color.multiplyLightness(lightness), depthPointConsumer)
    }

    private fun getNormal(points: List<D1Array<Double>>): D1Array<Double>{
        val v0 = points[0]
        val v1 = points[1]
        val v2 = points[2]
        val line1 =
            mk.ndarray(mk[v1[0].toDouble() - v0[0], v1[1].toDouble() - v0[1], v1[2].toDouble() - v0[2]])
        val line2 =
            mk.ndarray(mk[v2[0].toDouble() - v1[0], v2[1].toDouble() - v1[1], v2[2].toDouble() - v1[2]])
        return (line1 cross line2).normalized()
    }

}