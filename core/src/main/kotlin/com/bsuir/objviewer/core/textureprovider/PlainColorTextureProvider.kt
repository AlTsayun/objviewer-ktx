package com.bsuir.objviewer.core.textureprovider

import com.bsuir.objviewer.core.model.Color

class PlainColorTextureProvider(val color: Color) : TextureProvider {
    override fun get(x: Float, y: Float): Color  = color
}