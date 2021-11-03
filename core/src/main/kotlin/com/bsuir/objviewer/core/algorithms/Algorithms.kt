package com.bsuir.objviewer.core.algorithms

import com.bsuir.objviewer.core.extensions.dot
import com.bsuir.objviewer.core.extensions.pairedInCycle
import com.bsuir.objviewer.core.extensions.sign
import com.bsuir.objviewer.core.models.*
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.operations.minus
import java.lang.Math.abs

typealias DepthPointConsumer = (x: Int, y: Int, depth: UInt, color: Color) -> Unit

fun process(obj: WorldObject, world: World): ProcessedWorldObject = with(world) {
    val faces = ArrayList<ProcessedFace>()
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

            if (projectionV[2] < world.projectionNear || projectionV[2] > world.projectionFar) {
                isFaceOutOfProjection = true
                break
            }

            val point = FPoint2d(
                x = (windowSize.width - (viewportV[0] / viewportV[3])).toFloat(),
                y = (windowSize.height - (viewportV[1] / viewportV[3])).toFloat(),
            )

            if (point.x < 0 || point.x > windowSize.width ||
                point.y < 0 || point.y > windowSize.height) {
                isFaceOutOfProjection = true
                break
            }

            points.add(VertexProjections(modelV, viewV, projectionV, viewportV, point))

        }

//        if (!isFaceOutOfProjection) {
//            val v0 = points[0].model
//            val v1 = points[1].model
//            val v2 = points[2].model
//            val line1 = mk.ndarray(listOf(v1[0].toDouble() - v0[0], v1[1].toDouble() - v0[1], v1[2].toDouble() - v0[2]))
//            val line2 = mk.ndarray(listOf(v2[0].toDouble() - v1[0], v2[1].toDouble() - v1[1], v2[2].toDouble() - v1[2]))
//            val faceNormal = line1 cross line2
//            if (faceNormal dot (world.cam.target - world.cam.position) > 0) {
//                isFacePointsOutwards = true
//            }
//        }

        if (!isFaceOutOfProjection && !isFacePointsOutwards) {
            faces.add(ProcessedFace(points.map { ProcessedFace.Item(DepthPoint2d(it.point.x.toInt(), it.point.y.toInt(), it.projection[2].toUInt())) }))
        }

    }
    return ProcessedWorldObject(obj.id, faces)
}

fun drawLine(p0: DepthPoint2d,
             p1: DepthPoint2d,
             color: Color,
             consumer: DepthPointConsumer) {
    bresenham(p0, p1, color, consumer)
}

private fun bresenham(p0: DepthPoint2d,
                      p1: DepthPoint2d,
                      color: Color,
                      consumer: (x: Int, y: Int, depth: UInt, color: Color) -> Unit) {
    val deltaX = abs(p1.x - p0.x)
    val deltaY = abs(p1.y - p0.y)
    val deltaDepth = abs(p1.depth.toInt() - p0.depth.toInt())
    val dirX = sign(p1.x - p0.x)
    val dirY = sign(p1.y - p0.y)
    val dirDepth = sign(p1.depth.toInt() - p0.depth.toInt())

    var x = p0.x
    var y = p0.y
    var depth = p0.depth

    consumer(p0.x, p0.y, p0.depth, color)

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

fun drawFillFace(face: ProcessedFace, color: Color, consumer: DepthPointConsumer) {
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
                    DepthPoint2d(fromX, scanLine, fromDepth),
                    DepthPoint2d(toX, scanLine, toDepth),
                    color,
                    consumer
                )
            }
        }
    }

}


private fun getCurrentX(edge: Edge, y: Int): Int{
    return edge.minX + ((y - edge.minY) * edge.invSlope).toInt()
}

private fun getCurrentDepth(edge: Edge, y: Int): UInt{
    return edge.minDepth + ((y - edge.minY) * edge.invDepthSlope).toUInt()
}