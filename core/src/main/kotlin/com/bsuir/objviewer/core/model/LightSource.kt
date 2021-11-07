package com.bsuir.objviewer.core.model

import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.get

data class LightSource(val coords: D1Array<Double>){
    constructor(x: Double, y: Double, z: Double): this(mk.ndarray(listOf(x, y, z)))
    val x: Double = coords[0]
    val y: Double = coords[1]
    val z: Double = coords[2]
}