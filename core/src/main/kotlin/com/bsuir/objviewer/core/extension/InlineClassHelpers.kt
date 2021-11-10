@file:Suppress("NOTHING_TO_INLINE")
package com.bsuir.objviewer.core.extension

/**
 * Packs two Float values into one Long value for use in inline classes.
 */
inline fun packFloats(val1: Float, val2: Float): Long {
    val v1 = val1.toBits().toLong()
    val v2 = val2.toBits().toLong()
    return v1.shl(32) or (v2 and 0xFFFFFFFF)
}

/**
 * Unpacks the first Float value in [packFloats] from its returned Long.
 */
inline fun unpackFloat1(value: Long): Float {
    return Float.fromBits(value.shr(32).toInt())
}

/**
 * Unpacks the second Float value in [packFloats] from its returned Long.
 */
inline fun unpackFloat2(value: Long): Float {
    return Float.fromBits(value.and(0xFFFFFFFF).toInt())
}

/**
 * Packs two Int values into one Long value for use in inline classes.
 */
inline fun packInts(val1: Int, val2: Int): Long {
    return val1.toLong().shl(32) or (val2.toLong() and 0xFFFFFFFF)
}

/**
 * Unpacks the first Int value in [packInts] from its returned ULong.
 */
inline fun unpackInt1(value: Long): Int {
    return value.shr(32).toInt()
}

/**
 * Unpacks the second Int value in [packInts] from its returned ULong.
 */
inline fun unpackInt2(value: Long): Int {
    return value.and(0xFFFFFFFF).toInt()
}

/**
 * Packs four UByte values into one Int value for use in inline classes.
 */
inline fun packUBytes(val1: UByte, val2: UByte, val3: UByte, val4: UByte): Int {
    return val1.toInt().shl(24) or
            val2.toInt().shl(16) or
            val3.toInt().shl(8) or
            val4.toInt()
}

/**
 * Unpacks the first Byte value in [packUBytes] from its returned UInt.
 */
inline fun unpackUByte1(value: Int): UByte {
    return value.shr(24).toUByte()
}

/**
 * Unpacks the second UByte value in [packUBytes] from its returned UInt.
 */
inline fun unpackUByte2(value: Int): UByte {
    return value.shr(16).toUByte()
}

/**
 * Unpacks the third UByte value in [packUBytes] from its returned UInt.
 */
inline fun unpackUByte3(value: Int): UByte {
    return value.shr(8).toUByte()
}

/**
 * Unpacks the fourth UByte value in [packUBytes] from its returned UInt.
 */
inline fun unpackUByte4(value: Int): UByte {
    return value.and(0xFFFF).toUByte()
}