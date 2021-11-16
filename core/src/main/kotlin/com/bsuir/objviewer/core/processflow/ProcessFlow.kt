package com.bsuir.objviewer.core.processflow

import com.bsuir.objviewer.core.algorithm.getCenter
import com.bsuir.objviewer.core.extension.cross
import com.bsuir.objviewer.core.extension.normalized
import com.bsuir.objviewer.core.model.*
import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.operations.minus

class ProcessFlow(private val world: World) {

    private var viewMatrix = world.getViewMatrix()
    private var projectionMatrix = world.getProjectionMatrix()
    private var viewportMatrix = world.getViewportMatrix()
    private var objToModelMatrix = mutableMapOf<WorldObject, D2Array<Double>>()

    private var isMatricesOutdated = false

    init {
        world.subscribeOnChange(this::onWorldChanged)
    }

    private fun onWorldChanged(){
        isMatricesOutdated = true
    }

    private fun updateMatrices(){
        viewMatrix = world.getViewMatrix()
        projectionMatrix = world.getProjectionMatrix()
        viewportMatrix = world.getViewportMatrix()
    }

    fun process(consumer: (ProcessedFace) -> Unit) = with(world) {
        if (isMatricesOutdated){
            updateMatrices()
            isMatricesOutdated = false
        }

        for (obj in objects) {
            obj.faces.stream().parallel()
                .forEach { face ->
                    var isFaceOutOfProjection = false
                    var isFacePointsOutwards = false
                    val points = VertexesProjections(face.items.size)

                    for (vertex in face.items.map { it.vertex }) {
                        val modelV = mk.ndarray(listOf(vertex.x, vertex.y, vertex.z, vertex.w))
                        val worldV = obj.modelMatrix dot modelV
                        val viewV = viewMatrix dot worldV
                        val projectionV = projectionMatrix dot viewV
                        val viewportV = viewportMatrix dot projectionV

                        val point = DepthPoint2d(
                            x = (cam.windowSize.width - (viewportV[0] / viewportV[3])).toInt(),
                            y = (cam.windowSize.height - (viewportV[1] / viewportV[3])).toInt(),
                            depth = (viewportV[2] * 100).toUInt()
                        )
                        points.add(modelV, worldV, viewV, projectionV, viewportV, point)

                        if (projectionV[2] < cam.projectionNear) {
                            isFaceOutOfProjection = true
                        }
                    }

                    // sort out faces fully out of view
                    isFaceOutOfProjection = isFaceOutOfProjection || points.screen.all {
                        it.x < 0 || it.x > cam.windowSize.width ||
                                it.y < 0 || it.y > cam.windowSize.height
                    }

                    val faceNormal = getNormal(points.world)

                    // cos of an angle between camera and face normal
                    val camFaceCos = faceNormal dot getFaceDirection(cam.position, points.world)

                    if (camFaceCos > 0.0) {
                        isFacePointsOutwards = true
                    }

                    val invLightVectors = lightSources.map { light ->
                        getCenter(points.world)
                            .minus(light.coords)
                            .normalized()
                    }
                    val lightness = invLightVectors.map {
                        // cos of an angle between light source and face normal
                        val lightFaceCos = (it dot faceNormal)
                        val diffuse = (lightFaceCos + 1) / 2
                        val specular = 1 - (lightFaceCos - camFaceCos) / 2
                        return@map (diffuse + specular)
                    }.average()

                    if (!isFaceOutOfProjection && !isFacePointsOutwards) {
                        consumer.invoke(
                            ProcessedFace(
                                points.screen.map { ProcessedFace.Item(it) },
                                Color.GRAY.multiplyLightness(lightness)
                            )
                        )
                    }
                }

        }
    }

    private fun getNormal(points: List<D1Array<Double>>): D1Array<Double>{
        val v0 = points[0]
        val v1 = points[1]
        val v2 = points[2]
        val line1 =
            mk.ndarray(listOf(v1[0].toDouble() - v0[0], v1[1].toDouble() - v0[1], v1[2].toDouble() - v0[2]))
        val line2 =
            mk.ndarray(listOf(v2[0].toDouble() - v1[0], v2[1].toDouble() - v1[1], v2[2].toDouble() - v1[2]))
        return (line1 cross line2).normalized()
    }

    private fun getFaceDirection(from: D1Array<Double>, to: List<D1Array<Double>>): D1Array<Double>{
        return (to[0][0..3] - from).normalized()
    }
}