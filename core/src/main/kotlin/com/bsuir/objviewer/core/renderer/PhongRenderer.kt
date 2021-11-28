package com.bsuir.objviewer.core.renderer

import com.bsuir.objviewer.core.assetsprovider.TextureProvider
import com.bsuir.objviewer.core.extension.dot
import com.bsuir.objviewer.core.extension.normalized
import com.bsuir.objviewer.core.extension.pairedInCycle
import com.bsuir.objviewer.core.model.*
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.MultiArray
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.operations.div
import org.jetbrains.kotlinx.multik.ndarray.operations.minus
import org.jetbrains.kotlinx.multik.ndarray.operations.plus
import org.jetbrains.kotlinx.multik.ndarray.operations.times
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

class PhongRenderer(
    private val depthPointConsumer: DepthPointConsumer,
    var color: Color
): Renderer {

    private lateinit var camPosition: D1Array<Double>
    private lateinit var lightSources: List<LightSource>

    private val ambient = mk.ndarray(mk[0.1, 0.1, 0.1])
    private val albedoDiffuse = mk.ndarray(mk[0.5, 0.5, 0.5])
    private val albedoSpecular = mk.ndarray(mk[1.9, 1.9, 1.9])
    private val specularPower = 128


    fun setupWorldEnvironment(camPosition: D1Array<Double>, lightSources: List<LightSource>) {
        this.camPosition = camPosition
        this.lightSources = lightSources
    }

    override fun consumeFace(face: ProcessedFace) {
        drawFillFace(face, color, depthPointConsumer)
    }

    private fun dda(
        worldPoint1: D1Array<Double>,
        x1: Float,
        y1: Float,
        normal1: D1Array<Double>,
        depth1: Float,
        worldPoint2: D1Array<Double>,
        x2: Float,
        y2: Float,
        normal2: D1Array<Double>,
        depth2: Float,
        color: Color,
        consumer: DepthPointConsumer
    ) {
        val deltaWorldPoint = worldPoint2 - worldPoint1
        val deltaX = x2 - x1
        val deltaY = y2 - y1
        val deltaDepth = depth2 - depth1
        val deltaNormal = normal2 - normal1

        val delta = if (abs(deltaX) > abs(deltaY)) {
            abs(deltaX)
        } else {
            abs(deltaY)
        } + 1

        val dirX = deltaX / delta
        val dirY = deltaY / delta
        val dirDepth = deltaDepth / delta
        val dirNormal = deltaNormal / delta.toDouble()
        val dirWorldPoint = deltaWorldPoint / delta.toDouble()


        var x: Float = x1
        var y: Float = y1
        var depth = depth1
        var normal = normal1
        var worldPoint = worldPoint1

        for (i in 0..delta.toInt()) {

            val pointCamVector = (worldPoint - camPosition).normalized()

            val lightness = lightSources.map { lightSource ->
                val lightPointVector = (worldPoint - lightSource.coords).normalized()
                val reflectedLightVector = lightPointVector - normal * (2 * (lightPointVector dot normal))

                val diffuse = albedoDiffuse * max(lightPointVector dot normal, 0.0)
                val specular = albedoSpecular * max(reflectedLightVector dot pointCamVector, 0.0).pow(specularPower)

                return@map ambient + diffuse + specular
            }.reduce(MultiArray<Double, D1>::plus)

            consumer(
                x.toInt(),
                y.toInt(),
                depth,
                color.multiplyLightness(lightness[0], lightness[1], lightness[2], 1.0)
            )
            x += dirX
            y += dirY
            depth += dirDepth
            normal = (normal + dirNormal).normalized()
            worldPoint += dirWorldPoint
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
                        val fromWorldPoint = getCurrentWorldPoint(it[0], scanLine)
                        val fromX = getCurrentX(it[0], scanLine)
                        val fromDepth = getCurrentDepth(it[0], scanLine)
                        val fromNormal = getCurrentNormal(it[0], scanLine)
                        val toWorldPoint = getCurrentWorldPoint(it[1], scanLine)
                        val toX = getCurrentX(it[1], scanLine)
                        val toDepth = getCurrentDepth(it[1], scanLine)
                        val toNormal = getCurrentNormal(it[1], scanLine)
                        dda(
                            fromWorldPoint,
                            fromX.toFloat(),
                            scanLine.toFloat(),
                            fromNormal,
                            fromDepth,
                            toWorldPoint,
                            toX.toFloat(),
                            scanLine.toFloat(),
                            toNormal,
                            toDepth,
                            color,
                            consumer
                        )
                    }
            }
        }

    }

    private fun getCurrentWorldPoint(edge: Edge, y: Int): D1Array<Double> {
        return edge.lowerWorldPoint + (edge.invWorldPointSlope * (y - edge.lowerPoint.y).toDouble())
    }

    private fun getCurrentNormal(edge: Edge, y: Int): D1Array<Double> {
        return edge.lowerNormal + (edge.invNormalSlope * (y - edge.lowerPoint.y).toDouble())
    }

    private fun getCurrentX(edge: Edge, y: Int): Int{
        return edge.lowerPoint.x + (edge.invXSlope * (y - edge.lowerPoint.y) ).toInt()
    }

    private fun getCurrentDepth(edge: Edge, y: Int): Float{
        return edge.lowerPoint.depth + (edge.invDepthSlope * (y - edge.lowerPoint.y))
    }
}