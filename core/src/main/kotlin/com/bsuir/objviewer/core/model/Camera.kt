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
    private val subscribers: MutableList<Listener> = mutableListOf()

    fun subscribeOnChanged(action: Listener){
        subscribers.add(action)
    }

    private fun onChanged() = subscribers.forEach { it.invoke() }
    override fun toString(): String {
        return "Camera(subscribers=$subscribers, position=$position, front=$front, speed=$speed, windowSize=$windowSize, projectionNear=$projectionNear, projectionFar=$projectionFar, fieldOfView=$fieldOfView, up=$up)"
    }

    var position: D1Array<Double> = position
        set(value) {
            onChanged()
            field = value
        }
    var front: D1Array<Double> = front
        set(value) {
            onChanged()
            field = value
        }
    var speed: Double = speed
        set(value) {
            onChanged()
            field = value
        }
    var windowSize: IntSize = windowSize
        set(value) {
            onChanged()
            field = value
        }
    var projectionNear: Double = projectionNear
        set(value) {
            onChanged()
            field = value
        }
    var projectionFar: Double = projectionFar
        set(value) {
            onChanged()
            field = value
        }
    var fieldOfView: Double = fieldOfView
        set(value) {
            onChanged()
            field = value
        }

    val up: D1Array<Double> = mk.ndarray(listOf(0.0, 1.0, 0.0))
    val target: D1Array<Double>
        get() = position + front


}