package com.bsuir.objviewer.core.model

import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.operations.plus


class Camera(
    position: D1Array<Double>,
    front: D1Array<Double>,
    speed: Double,
    windowSize: IntSize,
    projectionNear: Double = 1.0,
    projectionFar: Double = 1000.0,
    fieldOfView: Double = 5.0,
) {
    var position: D1Array<Double> = position
        set(value) {
            isChanged = true
            target = value + front
            field = value
        }
    var front: D1Array<Double> = front
        set(value) {
            isChanged = true
            target = position + value
            field = value
        }
    var speed: Double = speed
        set(value) {
            isChanged = true
            field = value
        }
    var windowSize: IntSize = windowSize
        set(value) {
            isChanged = true
            field = value
        }
    var projectionNear: Double = projectionNear
        set(value) {
            isChanged = true
            field = value
        }
    var projectionFar: Double = projectionFar
        set(value) {
            isChanged = true
            field = value
        }
    var fieldOfView: Double = fieldOfView
        set(value) {
            isChanged = true
            field = value
        }
    var isChanged: Boolean = true


    val up: D1Array<Double> = mk.ndarray(listOf(0.0, 1.0, 0.0))
    var target: D1Array<Double> = position + front
        private set
}