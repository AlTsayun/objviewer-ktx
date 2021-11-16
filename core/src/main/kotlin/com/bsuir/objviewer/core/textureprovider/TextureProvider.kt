package com.bsuir.objviewer.core.textureprovider

import com.bsuir.objviewer.core.model.Color

interface TextureProvider {
    fun get(x: Float, y: Float): Color
}