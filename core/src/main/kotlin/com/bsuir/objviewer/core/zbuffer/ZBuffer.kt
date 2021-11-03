package com.bsuir.objviewer.core.zbuffer

import com.bsuir.objviewer.core.extensions.packInts
import com.bsuir.objviewer.core.extensions.unpackInt1
import com.bsuir.objviewer.core.extensions.unpackInt2
import com.bsuir.objviewer.core.models.Color
import java.util.ArrayList
import java.util.HashMap

@JvmInline
value class Coordinates internal constructor(private val packedValue: Long){
    val x: Int
        get() = unpackInt1(packedValue)

    val y: Int
        get() = unpackInt2(packedValue)
}
private fun Coordinates(x: Int, y: Int) = Coordinates(packInts(x,y))

class ZBuffer(width: Int, height: Int) {
    private val items: Array<Array<UInt>>
    private val _chunkedItems: MutableMap<Color, MutableList<Coordinates>>
    val chunkedItems: Map<Color, List<Coordinates>>
        get() = _chunkedItems

    init {
        items = Array(height) {
            Array(width) {UInt.MAX_VALUE
            }
        }
        _chunkedItems = HashMap()
    }

    operator fun get(i: Int): Array<UInt>{
        return items[i]
    }

    fun addPoint(x: Int, y: Int, depth: UInt, color: Color){

        if (this[y][x] > depth){
            if (_chunkedItems[color] == null) {
                _chunkedItems[color] = ArrayList()
            }
            _chunkedItems[color]?.add(Coordinates(x, y))
            this[y][x] = depth
        }
    }


//    fun transferTo(scope: DrawScope){
//        chunkedItems.forEach{ entry ->
//            val points = entry.value
//                .map { Offset(it.x.toFloat(), it.y.toFloat()) }
//            scope.drawPoints(points = points, pointMode = PointMode.Points, strokeWidth = 1f, color = entry.key)
//        }
//    }
}