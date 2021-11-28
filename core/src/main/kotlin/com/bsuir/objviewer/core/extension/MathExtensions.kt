package com.bsuir.objviewer.core.extension

import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.div
import org.jetbrains.kotlinx.multik.ndarray.operations.map
import org.jetbrains.kotlinx.multik.ndarray.operations.sum
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

/**
 * Normalizes vector by its length. For all zeros returns same vector.
 */
fun D1Array<Double>.normalized(): D1Array<Double> {
    val length = this.map { it.pow(2) }.sum()
    return if (length != 0.0) {
        this / sqrt(length)
    } else {
        this
    }
}

fun sign(a: Int) = when {
    a > 0 -> 1
    a < 0 -> -1
    else -> 0
}

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
/*internal*/ inline operator fun <T : Number> T.times(other: T): T = when (this) {
    is Byte -> (this.toByte() * other.toByte()).toByte()
    is Short -> (this.toShort() * other.toShort()).toShort()
    is Int -> (this.toInt() * other.toInt())
    is Long -> (this.toLong() * other.toLong())
    is Float -> (this.toFloat() * other.toFloat())
    is Double -> (this.toDouble() * other.toDouble())
    else -> throw Exception("Type not defined.")
} as T

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
/*internal*/ inline operator fun <T : Number> T.div(other: T): T = when (this) {
    is Byte -> (this.toByte() / other.toByte()).toByte()
    is Short -> (this.toShort() / other.toShort()).toShort()
    is Int -> (this.toInt() / other.toInt())
    is Long -> (this.toLong() / other.toLong())
    is Float -> (this.toFloat() / other.toFloat())
    is Double -> (this.toDouble() / other.toDouble())
    else -> throw Exception("Type not defined.")
} as T

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
/*internal*/ inline operator fun <T : Number> T.plus(other: T): T = when (this) {
    is Byte -> (this.toByte() + other.toByte()).toByte()
    is Short -> (this.toShort() + other.toShort()).toShort()
    is Int -> (this.toInt() + other.toInt())
    is Long -> (this.toLong() + other.toLong())
    is Float -> (this.toFloat() + other.toFloat())
    is Double -> (this.toDouble() + other.toDouble())
    else -> throw Exception("Type not defined.")
} as T

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
/*internal*/ inline operator fun <T : Number> T.minus(other: T): T = when (this) {
    is Byte -> (this.toByte() - other.toByte()).toByte()
    is Short -> (this.toShort() - other.toShort()).toShort()
    is Int -> (this.toInt() - other.toInt())
    is Long -> (this.toLong() - other.toLong())
    is Float -> (this.toFloat() - other.toFloat())
    is Double -> (this.toDouble() - other.toDouble())
    else -> throw Exception("Type not defined.")
} as T
//
//@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
///*internal*/ inline operator fun UByte.times(other: Number): UByte = (this.toDouble() * other).toByte().toUByte()
//
//@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
///*internal*/ inline operator fun UByte.div(other: Number): UByte = (this.toByte() / other).toByte().toUByte()
//
//@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
///*internal*/ inline operator fun UByte.plus(other: Number): UByte = (this.toByte() + other).toByte().toUByte()
//
//@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
///*internal*/ inline operator fun UByte.plus(other: UByte): UByte = (this.toInt() + other.toInt()).toByte().toUByte()
//
//@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
///*internal*/ inline operator fun UByte.minus(other: Number): UByte = (this.toByte() - other).toByte().toUByte()

infix fun UByte.timesNoOverflow(value: Double): UByte{
    return (this.toInt() * value)
        .let {
            if (it < UByte.MIN_VALUE.toInt()){
                UByte.MIN_VALUE
            } else if (it > UByte.MAX_VALUE.toInt()){
                UByte.MAX_VALUE
            } else {
                it.toInt().toUByte()
            }
        }
}

infix fun UByte.plusNoOverflow(value: UByte): UByte{
    return (this + value)
        .toInt()
        .let {
            if (it > UByte.MAX_VALUE.toInt()){
                UByte.MAX_VALUE
            } else {
                it.toUByte()
            }
        }
}

fun UInt.minusNoOverflow(value: Int): UInt{
    return (this.toInt() - value)
        .let {
            if (it < UInt.MIN_VALUE.toInt()){
                UInt.MIN_VALUE
            } else if (it > UInt.MAX_VALUE.toInt()){
                UInt.MIN_VALUE
            } else {
                it.toUInt()
            }
        }
}