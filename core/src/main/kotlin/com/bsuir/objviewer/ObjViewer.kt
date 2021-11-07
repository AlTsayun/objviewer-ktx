package com.bsuir.objviewer

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.bsuir.objviewer.core.model.World
import com.bsuir.objviewer.screen.CanvasScreen
import com.bsuir.objviewer.service.ObjParser
import com.bsuir.objviewer.service.WorldCreator
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.app.emptyScreen
import java.util.*

/** [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms.  */
class ObjViewer : KtxGame<KtxScreen>() {
    val batch by lazy { SpriteBatch() }

    override fun create() {
        addScreen(CanvasScreen(this, WorldCreator().world))
        setScreen<CanvasScreen>()
        super.create()
    }

    override fun dispose() {
        batch.dispose()
        super.dispose()
    }
}