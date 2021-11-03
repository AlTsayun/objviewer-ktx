package com.bsuir.objviewer

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import java.util.*

/** [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms.  */
class ObjViewer : ApplicationAdapter() {
    private lateinit var batch: SpriteBatch
    private lateinit var image: Texture

    override fun create() {
        batch = SpriteBatch()
        image = Texture("libgdx.png")
    }

    override fun render() {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        batch.begin()
        batch.draw(image, 140f, 210f)
        batch.end()
    }

    override fun dispose() {
        batch.dispose()
        image.dispose()
    }
}