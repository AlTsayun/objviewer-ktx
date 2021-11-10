package com.bsuir.objviewer.core.processflow

import com.bsuir.objviewer.core.algorithm.getCenter
import com.bsuir.objviewer.core.extension.cross
import com.bsuir.objviewer.core.extension.normalized
import com.bsuir.objviewer.core.model.*
import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.operations.minus

class ProcessFlow(private val world: World) {

    private var viewMatrix = world.getViewMatrix()
    private var projectionMatrix = world.getProjectionMatrix()
    private var viewportMatrix = world.getViewportMatrix()
    private var objToModelMatrix = mutableMapOf<WorldObject, D2Array<Double>>()

    private var isMatricesOutDated = false

    init {
        world.subscribeOnChange(this::onWorldChanged)
    }

    private fun onWorldChanged(){
        isMatricesOutDated = true
    }

    private fun updateMatrices(){
        viewMatrix = world.getViewMatrix()
        projectionMatrix = world.getProjectionMatrix()
        viewportMatrix = world.getViewportMatrix()
    }

    fun process(consumer: (ProcessedWorldObject) -> Unit) = with(world) {
        if (isMatricesOutDated){
            updateMatrices()
            isMatricesOutDated = false
        }
        for (obj in world.objects) {
            val faces = ArrayList<ProcessedFace>()

            for (face in obj.faces) {
                var isFaceOutOfProjection = false
                var isFacePointsOutwards = false
                val points = ArrayList<VertexProjections>(face.items.size)

                for (vertex in face.items.map { it.vertex }) {
                    val modelV = mk.ndarray(listOf(vertex.x, vertex.y, vertex.z, vertex.w))
                    val worldV = obj.modelMatrix dot modelV
                    val viewV = viewMatrix dot worldV
                    val projectionV = projectionMatrix dot viewV
                    val viewportV = viewportMatrix dot projectionV

                    val point = FPoint2d(
                        x = (cam.windowSize.width - (viewportV[0] / viewportV[3])).toFloat(),
                        y = (cam.windowSize.height - (viewportV[1] / viewportV[3])).toFloat(),
                    )

                    val projections = VertexProjections(modelV, worldV, viewV, projectionV, viewportV, point)
                    points.add(projections)

                    if (projectionV[2] < world.cam.projectionNear || projectionV[2] > world.cam.projectionFar) {
                        isFaceOutOfProjection = true
                    }

                    if (point.x < 0 || point.x > cam.windowSize.width ||
                        point.y < 0 || point.y > cam.windowSize.height
                    ) {
                        isFaceOutOfProjection = true
                    }
                }

                val v0 = points[0].world
                val v1 = points[1].world
                val v2 = points[2].world
                val line1 =
                    mk.ndarray(listOf(v1[0].toDouble() - v0[0], v1[1].toDouble() - v0[1], v1[2].toDouble() - v0[2]))
                val line2 =
                    mk.ndarray(listOf(v2[0].toDouble() - v1[0], v2[1].toDouble() - v1[1], v2[2].toDouble() - v1[2]))
                val faceNormal = (line1 cross line2).normalized()

                if (faceNormal dot (world.cam.target - world.cam.position).normalized() > 0.0) {
                    isFacePointsOutwards = true
                }

                val invLightVectors = lightSources.map { light ->
                    getCenter(points.map { it.world })
                        .minus(light.coords)
                        .normalized()
                }
                val lightness = invLightVectors.map { ((it dot faceNormal) + 1) / 2 }.average()

                if (!isFaceOutOfProjection && !isFacePointsOutwards) {
                    faces.add(
                        ProcessedFace(
                            points.map {
                                ProcessedFace.Item(
                                    DepthPoint2d(
                                        it.point.x.toInt(),
                                        it.point.y.toInt(),
                                        (it.projection[2]).toUInt()
                                    )
                                )
                            },
                            Color.WHITE.multiplyLightness(lightness)
                        )
                    )
                }
            }
            consumer.invoke(ProcessedWorldObject(obj.id, faces))

        }
    }
}