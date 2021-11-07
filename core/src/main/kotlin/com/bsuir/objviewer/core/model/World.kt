package com.bsuir.objviewer.core.model

import com.bsuir.objviewer.core.extension.cross
import com.bsuir.objviewer.core.extension.dot
import com.bsuir.objviewer.core.extension.normalized
import com.bsuir.objviewer.core.model.ObjEntry.*
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.operations.minus
import org.jetbrains.kotlinx.multik.ndarray.operations.plus
import java.util.*
import kotlin.math.tan

data class World(
    val objects: MutableList<WorldObject>,
    val lightSources: MutableList<LightSource>,
    val cam: Camera,
) {
    fun getViewMatrix(): D2Array<Double> {
            val zAxis = (cam.position - cam.target).normalized()
            val xAxis = (cam.up cross zAxis).normalized()
            val yAxis = (zAxis cross xAxis).normalized()
            return mk.ndarray(
                listOf(
                    listOf(xAxis[0], xAxis[1], xAxis[2], -(xAxis dot cam.position)),
                    listOf(yAxis[0], yAxis[1], yAxis[2], -(yAxis dot cam.position)),
                    listOf(zAxis[0], zAxis[1], zAxis[2], -(zAxis dot cam.position)),
                    listOf(0.0, 0.0, 0.0, 1.0),
                )
            )
        }

    fun getViewportMatrix(): D2Array<Double> {
            val windowWidth = cam.windowSize.width.toDouble()
            val windowHeight = cam.windowSize.height.toDouble()
            val windowXMin = 0
            val windowYMin = 0
            return mk.ndarray(
                listOf(
                    listOf(windowWidth / 2, 0.0, 0.0, windowXMin + windowWidth / 2),
                    listOf(0.0, -windowHeight / 2, 0.0, windowYMin + windowHeight / 2),
                    listOf(0.0, 0.0, 1.0, 0.0),
                    listOf(0.0, 0.0, 0.0, 1.0),
                )
            )
        }

    fun getProjectionMatrix() : D2Array<Double> {
            val aspect = cam.windowSize.width.toDouble() / cam.windowSize.height
            return mk.ndarray(
                listOf(
                    listOf(1 / (aspect * tan(cam.fieldOfView / 2.0)), 0.0, 0.0, 0.0),
                    listOf(0.0, 1 / tan(cam.fieldOfView / 2.0), 0.0, 0.0),
                    listOf(
                        0.0,
                        0.0,
                        cam.projectionFar / (cam.projectionNear - cam.projectionFar),
                        cam.projectionNear * cam.projectionFar / (cam.projectionNear - cam.projectionFar)
                    ),
                    listOf(0.0, 0.0, -1.0, 0.0),
                )
            )
        }
}

data class WorldObject(
    val id: UUID,
    val vertexes: List<Vertex>,
    val textures: List<Texture>,
    val normals: List<Normal>,
    val faces: List<Face>,
) {
    val modelMatrix: D2Array<Double> = mk.ndarray(
        listOf(
            listOf(1.0, 0.0, 0.0, 0.0),
            listOf(0.0, 1.0, 0.0, 0.0),
            listOf(0.0, 0.0, 1.0, 0.0),
            listOf(0.0, 0.0, 0.0, 1.0),
        )
    )
}
