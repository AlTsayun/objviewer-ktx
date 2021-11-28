package com.bsuir.objviewer.core.renderer

import com.bsuir.objviewer.core.assetsprovider.NormalProvider
import com.bsuir.objviewer.core.assetsprovider.SpecularProvider
import com.bsuir.objviewer.core.assetsprovider.TextureProvider
import com.bsuir.objviewer.core.extension.dot
import com.bsuir.objviewer.core.extension.normalized
import com.bsuir.objviewer.core.extension.pairedInCycle
import com.bsuir.objviewer.core.model.Edge
import com.bsuir.objviewer.core.model.FPoint2d
import com.bsuir.objviewer.core.model.LightSource
import com.bsuir.objviewer.core.model.ProcessedFace
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

class FullPackedRenderer(
    private val depthPointConsumer: DepthPointConsumer
): Renderer {

    private lateinit var camPosition: D1Array<Double>
    private lateinit var lightSources: List<LightSource>
    private lateinit var textureProvider: TextureProvider
    private lateinit var normalProvider: NormalProvider
    private lateinit var specularProvider: SpecularProvider

    private val ambient = mk.ndarray(mk[0.5, 0.5, 0.5])
    private val albedoDiffuse = mk.ndarray(mk[0.4, 0.4, 0.4])
    private val albedoSpecular = mk.ndarray(mk[0.3, 0.3, 0.3])
    private val specularPower = 8


    fun setupWorldEnvironment(
        camPosition: D1Array<Double>,
        lightSources: List<LightSource>,
        textureProvider: TextureProvider,
        normalProvider: NormalProvider,
        specularProvider: SpecularProvider
    ) {
        this.camPosition = camPosition
        this.lightSources = lightSources
        this.textureProvider = textureProvider
        this.normalProvider = normalProvider
        this.specularProvider = specularProvider
    }

    override fun consumeFace(face: ProcessedFace) {
        drawFillFace(face, textureProvider, normalProvider, specularProvider, depthPointConsumer)
    }

    private fun dda(
        worldPoint1: D1Array<Double>,
        x1: Float,
        y1: Float,
        xt1: Float,
        yt1: Float,
        depth1: Float,
        worldPoint2: D1Array<Double>,
        x2: Float,
        y2: Float,
        xt2: Float,
        yt2: Float,
        depth2: Float,
        textureProvider: TextureProvider,
        normalProvider: NormalProvider,
        specularProvider: SpecularProvider,
        consumer: DepthPointConsumer
    ) {
        val deltaWorldPoint = worldPoint2 - worldPoint1
        val deltaX = x2 - x1
        val deltaXt = xt2 - xt1
        val deltaY = y2 - y1
        val deltaYt = yt2 - yt1
        val deltaDepth = depth2 - depth1

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
        val dirWorldPoint = deltaWorldPoint / delta.toDouble()


        var x: Float = x1
        var y: Float = y1
        var xt = xt1
        var yt = yt1
        var depth = depth1
        var worldPoint = worldPoint1

        for (i in 0..delta.toInt()) {

            val pointCamVector = (worldPoint - camPosition).normalized()
            val normal = normalProvider.get(xt, yt)
            val specularMagnifier = specularProvider.get(xt, yt)

            val lightness = lightSources.map { lightSource ->
                val lightPointVector = (worldPoint - lightSource.coords).normalized()
                val reflectedLightVector = lightPointVector - normal * (2 * (lightPointVector dot normal))

                val diffuse = albedoDiffuse * max(lightPointVector dot normal, 0.0)
                val specular = albedoSpecular * (max(reflectedLightVector dot pointCamVector, 0.0).pow(specularPower) * specularMagnifier)

                return@map ambient + diffuse + specular
            }.reduce(MultiArray<Double, D1>::plus)

            consumer(
                x.toInt(),
                y.toInt(),
                depth,
                textureProvider.get(xt, yt)
                    .multiplyLightness(lightness[0], lightness[1], lightness[2], 1.0)
            )
            x += dirX
            y += dirY
            xt += dirXt
            yt += dirYt
            depth += dirDepth
            worldPoint += dirWorldPoint
        }
    }

    fun drawFillFace(
        face: ProcessedFace,
        textureProvider: TextureProvider,
        normalProvider: NormalProvider,
        specularProvider: SpecularProvider,
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
                        val fromTexture = getCurrentTexture(it[0], scanLine)
                        val toWorldPoint = getCurrentWorldPoint(it[1], scanLine)
                        val toX = getCurrentX(it[1], scanLine)
                        val toDepth = getCurrentDepth(it[1], scanLine)
                        val toTexture = getCurrentTexture(it[1], scanLine)
                        dda(
                            fromWorldPoint,
                            fromX.toFloat(),
                            scanLine.toFloat(),
                            fromTexture.x,
                            fromTexture.y,
                            fromDepth,
                            toWorldPoint,
                            toX.toFloat(),
                            scanLine.toFloat(),
                            toTexture.x,
                            toTexture.y,
                            toDepth,
                            textureProvider,
                            normalProvider,
                            specularProvider,
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

    private fun getCurrentTexture(edge: Edge, y: Int): FPoint2d {
        return FPoint2d(
            edge.lowerTexture.x + ((y - edge.lowerPoint.y) * edge.invTextureXSlope),
            edge.lowerTexture.y + ((y - edge.lowerPoint.y) * edge.invTextureYSlope)
        )

    }
}