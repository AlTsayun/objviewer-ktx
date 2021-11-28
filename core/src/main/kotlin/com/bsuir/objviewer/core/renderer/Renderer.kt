package com.bsuir.objviewer.core.renderer

import com.bsuir.objviewer.core.model.*

typealias DepthPointConsumer = (x: Int, y: Int, depth: Float, color: Color) -> Unit

interface Renderer {
    fun consumeFace(face: ProcessedFace)
}