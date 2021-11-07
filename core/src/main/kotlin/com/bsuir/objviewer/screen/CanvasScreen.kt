package com.bsuir.objviewer.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.bsuir.objviewer.ObjViewer
import com.bsuir.objviewer.core.algorithm.drawFillFace
import com.bsuir.objviewer.core.algorithm.drawStrokeFace
import com.bsuir.objviewer.core.algorithm.process
import com.bsuir.objviewer.core.extension.cross
import com.bsuir.objviewer.core.extension.normalized
import com.bsuir.objviewer.core.model.Color
import com.bsuir.objviewer.core.model.IntSize
import com.bsuir.objviewer.core.model.World
import com.bsuir.objviewer.core.zbuffer.ZBuffer
import ktx.app.KtxScreen
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.operations.minus
import org.jetbrains.kotlinx.multik.ndarray.operations.plus
import org.jetbrains.kotlinx.multik.ndarray.operations.times
import kotlin.math.cos
import kotlin.math.sin


class CanvasScreen(private val application: ObjViewer, private val world: World) : KtxScreen {

    private val cam = world.cam
    private var mouseSensitivity = 0.5
    private var yaw = -90.0
    private var pitch = 0.0
    private val zBuffer = ZBuffer(
        cam.windowSize.width,
        cam.windowSize.height,
        Color(UByte.MAX_VALUE, UByte.MAX_VALUE, UByte.MAX_VALUE, UByte.MAX_VALUE)
    )

    private val dragProcessor = object : InputAdapter() {
        private var touchX = 0
        private var touchY = 0
        var deltaX = 0
            private set
        var deltaY = 0
            private set

        fun isDragged(): Boolean = (deltaX != 0) && (deltaY != 0)

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            touchX = screenX
            touchY = screenY
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            deltaX = screenX - touchX
            deltaY = screenY - touchY
            return true
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            return true
        }

        fun getDeltaXAndReset(): Int{
            val value = deltaX
            touchX += deltaX
            deltaX = 0
            return value
        }

        fun getDeltaYAndReset(): Int{
            val value = deltaY
            touchY += deltaY
            deltaY = 0
            return value
        }
    }

    init {
        Gdx.input.inputProcessor = dragProcessor
    }

    override fun render(delta: Float) {

        if (cam.isChanged){
            zBuffer.invalidate()
            world.objects
                .map { process(it, world) }
                .forEach { obj ->
                    obj.faces.forEach { face ->
                        drawFillFace(face, Color(0u, 0u, UByte.MAX_VALUE, UByte.MAX_VALUE), zBuffer::addPoint)
//                        drawStrokeFace(face, Color(0u, 0u, UByte.MAX_VALUE, UByte.MAX_VALUE), zBuffer::addPoint)
                    }
                }
            cam.isChanged = false
        }

        val pixmap = Pixmap(zBuffer.width, zBuffer.height, Pixmap.Format.RGBA8888)
        zBuffer.transferTo { x, y, color ->
            pixmap.drawPixel(x, y, color.packedValue)
        }

        val img = Texture(pixmap)
        pixmap.dispose()

        application.batch.begin()
        application.batch.draw(img, 0f, 0f)
        application.batch.end()

        processInput()
    }

    private fun processInput() {
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            cam.position = cam.position + cam.front * cam.speed
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            cam.position = cam.position - cam.front * cam.speed
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            cam.position = cam.position - (cam.front cross cam.up).normalized() * cam.speed
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            cam.position = cam.position + (cam.front cross cam.up).normalized() * cam.speed
        }

        if (dragProcessor.isDragged()){
            yaw += (dragProcessor.getDeltaXAndReset()) * mouseSensitivity
            pitch += (dragProcessor.getDeltaYAndReset()) * mouseSensitivity

            // make sure that when pitch is out of bounds, screen doesn't get flipped
            if (pitch > 89.0) pitch = 89.0
            if (pitch < -89.0) pitch = -89.0

            cam.front = mk.ndarray(
                listOf(
                    cos(Math.toRadians(yaw)) * cos(Math.toRadians(pitch)),
                    sin(Math.toRadians(pitch)),
                    sin(Math.toRadians(yaw)) * cos(Math.toRadians(pitch)),
                )
            ).normalized()
        }
    }
}