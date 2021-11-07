package com.bsuir.objviewer.core.algorithm

import com.bsuir.objviewer.core.extension.cross
import com.bsuir.objviewer.core.extension.normalized
import com.bsuir.objviewer.core.extension.pairedInCycle
import com.bsuir.objviewer.core.extension.sign
import com.bsuir.objviewer.core.model.*
import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.operations.minus
import kotlin.math.abs

typealias DepthPointConsumer = (x: Int, y: Int, depth: UInt, color: Color) -> Unit

fun process(obj: WorldObject, world: World): ProcessedWorldObject = with(world) {
    val faces = ArrayList<ProcessedFace>()

    val viewMatrix = getViewMatrix()
    val projectionMatrix = getProjectionMatrix()
    val viewportMatrix = getViewportMatrix()

    for (face in obj.faces) {
        var isFaceOutOfProjection = false
        var isFacePointsOutwards = false
        val points = ArrayList<VertexProjections>()

        for (vertex in face.items.map { it.vertex }) {
            val v = mk.ndarray(listOf(vertex.x, vertex.y, vertex.z, vertex.w))
            val modelV = obj.modelMatrix dot v
            val viewV = viewMatrix dot modelV
            val projectionV = projectionMatrix dot viewV
            val viewportV = viewportMatrix dot projectionV

            if (projectionV[2] < world.cam.projectionNear || projectionV[2] > world.cam.projectionFar) {
                isFaceOutOfProjection = true
            }

            val point = FPoint2d(
                x = (cam.windowSize.width - (viewportV[0] / viewportV[3])).toFloat(),
                y = (cam.windowSize.height - (viewportV[1] / viewportV[3])).toFloat(),
            )

            if (point.x < 0 || point.x > cam.windowSize.width ||
                point.y < 0 || point.y > cam.windowSize.height) {
                isFaceOutOfProjection = true
            }

            points.add(VertexProjections(modelV, viewV, projectionV, viewportV, point))
        }

        val v0 = points[0].model
        val v1 = points[1].model
        val v2 = points[2].model
        val line1 = mk.ndarray(listOf(v1[0].toDouble() - v0[0], v1[1].toDouble() - v0[1], v1[2].toDouble() - v0[2]))
        val line2 = mk.ndarray(listOf(v2[0].toDouble() - v1[0], v2[1].toDouble() - v1[1], v2[2].toDouble() - v1[2]))
        val faceNormal = (line1 cross line2).normalized()

        if (!isFaceOutOfProjection) {
            if (faceNormal dot (world.cam.target - world.cam.position).normalized() > 0.0) {
                isFacePointsOutwards = true
            }
        }

        val invLightVectors = lightSources.map { light ->
            (getCenter(points.map { it.model }) - light.coords).normalized()
        }
        val lightness = invLightVectors.map { ((it dot faceNormal) + 1) / 2 }.average()

        if (!isFaceOutOfProjection && !isFacePointsOutwards) {
            faces.add(ProcessedFace(
                points.map {
                    ProcessedFace.Item(DepthPoint2d(it.point.x.toInt(), it.point.y.toInt(), it.projection[2].toUInt()))
                },
                Color(UByte.MAX_VALUE, UByte.MAX_VALUE, UByte.MAX_VALUE, UByte.MAX_VALUE).multiplyLightness(lightness)
            ))
        }
    }
    return ProcessedWorldObject(obj.id, faces)
}

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
    depth1: UInt,
    x2: Int,
    y2: Int,
    depth2: UInt,
    color: Color,
    consumer: DepthPointConsumer
) {
    bresenham(x1, y1, depth1, x2, y2, depth2, color, consumer)
}

private fun bresenham(
    x1: Int,
    y1: Int,
    depth1: UInt,
    x2: Int,
    y2: Int,
    depth2: UInt,
    color: Color,
    consumer: (x: Int, y: Int, depth: UInt, color: Color) -> Unit
) {
    val deltaX = abs(x2 - x1)
    val deltaY = abs(y2 - y1)
    val deltaDepth = abs(depth2.toInt() - depth1.toInt())
    val dirX = sign(x2 - x1)
    val dirY = sign(y2 - y1)
    val dirDepth = sign(depth2.toInt() - depth1.toInt())

    var x = x1
    var y = y1
    var depth = depth1

    consumer(x1, y1, depth1, color)

    if (deltaX >= deltaY && deltaX >= deltaDepth) {
        //iteration by x
        var errorY = 2 * deltaY - deltaX
        var errorDepth = 2 * deltaDepth - deltaX
        for (i: Int in 1..deltaX) {
            x += dirX
            if (errorY >= 0) {
                y += dirY
                errorY -= 2 * deltaX
            }
            if (errorDepth >= 0) {
                depth = (depth.toInt() - dirDepth).toUInt()
                errorDepth -= 2 * deltaX
            }
            errorY += 2 * deltaY
            errorDepth += 2 * deltaDepth
            consumer(x, y, depth, color)
        }
    } else if (deltaY >= deltaX && deltaY >= deltaDepth) {
        //iteration by y
        var errorX = 2 * deltaX - deltaY
        var errorDepth = 2 * deltaDepth - deltaY
        for (i: Int in 1..deltaY) {
            y += dirY
            if (errorX >= 0) {
                x += dirX
                errorX -= 2 * deltaY
            }
            if (errorDepth >= 0) {
                depth = (depth.toInt() - dirDepth).toUInt()
                errorDepth -= 2 * deltaY
            }
            errorX += 2 * deltaX
            errorDepth += 2 * deltaDepth
            consumer(x, y, depth, color)
        }
    } else {
        //iteration by depth
        var errorX = 2 * deltaX - deltaDepth
        var errorY = 2 * deltaY - deltaDepth
        for (i: Int in 1..deltaDepth) {
            depth = (depth.toInt() - dirDepth).toUInt()
            if (errorX >= 0) {
                x += dirX
                errorX -= 2 * deltaDepth
            }
            if (errorY >= 0) {
                y += dirY
                errorY -= 2 * deltaDepth
            }
            errorX += 2 * deltaX
            errorY += 2 * deltaY
            consumer(x, y, depth, color)
        }
    }
}

fun drawFillFace(
    face: ProcessedFace,
    consumer: DepthPointConsumer
) {
    val allEdges = face.items
        .pairedInCycle()
        .map { pair ->
            val i1 = pair.first
            val i2 = pair.second
            val sorted = if (i1.point.y < i2.point.y) {
                i1.point to i2.point
            } else {
                i2.point to i1.point
            }
            Edge(
                sorted.first.x,
                sorted.first.y,
                sorted.first.depth,
                sorted.second.y,
                (sorted.second.depth - sorted.first.depth).toFloat() / (sorted.second.y - sorted.first.y).toFloat(),
                (sorted.second.x - sorted.first.x) / (sorted.second.y - sorted.first.y).toFloat()
            )
        }

    val yComparator = compareBy<Edge> { it.minY }
    val xComparator = compareBy<Edge> { it.minX }
    //todo: fix comparing float to 0
    val globalEdges = allEdges
//        .filter { it.invSlope != 0f }
        .sortedWith(yComparator.then(xComparator))

    if (globalEdges.isNotEmpty()) {
        for (scanLine: Int in globalEdges.first().minY..globalEdges.last().maxY){
            val activeEdges = globalEdges
                .filter { (it.minY <= scanLine) && (it.maxY >= scanLine) }
                .chunked(2)
                .filter { it.size == 2 }

            activeEdges.forEach {
                val fromX = getCurrentX(it[0], scanLine)
                val toX = getCurrentX(it[1], scanLine)
                val fromDepth = getCurrentDepth(it[0], scanLine)
                val toDepth = getCurrentDepth(it[1], scanLine)
                drawLine(
                    fromX,
                    scanLine,
                    fromDepth,
                    toX,
                    scanLine,
                    toDepth,
                    face.color,
                    consumer
                )
            }
        }
    }

}

private fun getCurrentX(edge: Edge, y: Int): Int{
    return edge.minX + ((y - edge.minY) * edge.invXSlope).toInt()
}

private fun getCurrentDepth(edge: Edge, y: Int): UInt{
    return edge.minDepth + ((edge.minY - y) * edge.invDepthSlope).toUInt()
}

fun drawStrokeFace(face: ProcessedFace, consumer: DepthPointConsumer){
    face.items
        .pairedInCycle()
        .forEach { pair ->
            drawLine(
                pair.first.point.x,
                pair.first.point.y,
                pair.first.point.depth,
                pair.second.point.x,
                pair.second.point.y,
                pair.second.point.depth,
                face.color,
                consumer
            )
        }
}