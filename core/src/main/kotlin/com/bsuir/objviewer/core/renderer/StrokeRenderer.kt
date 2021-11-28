package com.bsuir.objviewer.core.renderer

import com.bsuir.objviewer.core.extension.pairedInCycle
import com.bsuir.objviewer.core.model.Color
import com.bsuir.objviewer.core.model.ProcessedFace
import kotlin.math.abs


class StrokeRenderer(
    private val depthPointConsumer: DepthPointConsumer,
    var color: Color
): Renderer {

    private fun drawLine(
        x1: Int,
        y1: Int,
        depth1: Float,
        x2: Int,
        y2: Int,
        depth2: Float,
        color: Color,
        consumer: DepthPointConsumer
    ) {
        dda(
            x1.toFloat(),
            y1.toFloat(),
            depth1,
            x2.toFloat(),
            y2.toFloat(),
            depth2,
            color,
            consumer
        )
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

    override fun consumeFace(face: ProcessedFace) {
        face.vertices.map {
            it.point
        }.pairedInCycle().forEach {
            drawLine(
                it.first.x,
                it.first.y,
                it.first.depth,
                it.second.x,
                it.second.y,
                it.second.depth,
                color,
                depthPointConsumer
            )
        }
    }

}
