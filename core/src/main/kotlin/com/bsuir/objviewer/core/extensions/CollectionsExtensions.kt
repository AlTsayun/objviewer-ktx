package com.bsuir.objviewer.core.extensions

import java.util.ArrayList

fun <T> paired(list: Collection<T>): List<Pair<T, T>> {
    val pairs: MutableList<Pair<T, T>> = ArrayList(list.size)
    list.stream().reduce { a: T, b: T ->
        pairs.add(a to b)
        return@reduce b
    }
    return pairs
}

fun <T> List<T>.pairedInCycle(): List<Pair<T, T>> {
    val pairs: MutableList<Pair<T, T>> = ArrayList(this.size)
    this.stream().reduce { a: T, b: T ->
        pairs.add(Pair(a, b))
        return@reduce b
    }
    pairs.add(Pair(this.last(), this.first()))
    return pairs
}