package com.bsuir.objviewer.core.extension

import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.div
import kotlin.math.pow
import kotlin.math.sqrt

infix fun <T: Number> D1Array<T>.dot(another: D1Array<T>): T =
    mk.linalg.dot(this, another)

inline infix fun <reified T: Number> D1Array<T>.cross(another: D1Array<T>): D1Array<T> {
    require(this.size == 3)
    require(another.size == 3)
    return mk.ndarray(
        listOf(
            (this[1] * another[2]) - (this[2] * another[1]),
            this[2] * another[0] - this[0] * another[2],
            this[0] * another[1] - this[1] * another[0],
        )
    )
}

fun D1Array<Double>.normalized(): D1Array<Double> {
    require(this.size == 3)
    return this / sqrt(this[0].pow(2) + this[1].pow(2) + this[2].pow(2))
}

fun sign(a: Int) = when {
    a > 0 -> 1
    a < 0 -> -1
    else -> 0
}


@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
/*internal*/ inline operator fun <T : Number> Number.times(other: T): T = when (this) {
    is Byte -> (this.toByte() * other.toByte()).toByte()
    is Short -> (this.toShort() * other.toShort()).toShort()
    is Int -> (this.toInt() * other.toInt())
    is Long -> (this.toLong() * other.toLong())
    is Float -> (this.toFloat() * other.toFloat())
    is Double -> (this.toDouble() * other.toDouble())
    else -> throw Exception("Type not defined.")
} as T


@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
/*internal*/ inline operator fun <T : Number> Number.minus(other: T): T = when (this) {
    is Byte -> (this.toByte() - other.toByte()).toByte()
    is Short -> (this.toShort() - other.toShort()).toShort()
    is Int -> (this.toInt() - other.toInt())
    is Long -> (this.toLong() - other.toLong())
    is Float -> (this.toFloat() - other.toFloat())
    is Double -> (this.toDouble() - other.toDouble())
    else -> throw Exception("Type not defined.")
} as T