package com.bsuir.objviewer.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.bsuir.objviewer.ObjViewer
import com.bsuir.objviewer.core.algorithm.DepthPointConsumer
import com.bsuir.objviewer.core.algorithm.drawFillFace
import com.bsuir.objviewer.core.algorithm.drawStrokeFace
import com.bsuir.objviewer.core.extension.cross
import com.bsuir.objviewer.core.extension.normalized
import com.bsuir.objviewer.core.model.Color
import com.bsuir.objviewer.core.model.World
import com.bsuir.objviewer.core.processflow.ProcessFlow
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
    private val viewSensitivity = 1
    private var yaw = -90.0
    private var pitch = 0.0
    private val zBuffer = ZBuffer(
        cam.windowSize.width,
        cam.windowSize.height,
        Color.WHITE
    )

    private var worldChanged = true

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

    private val processFlow: ProcessFlow = ProcessFlow(world)

    init {
        Gdx.input.inputProcessor = dragProcessor
        world.subscribeOnChange {
            worldChanged = true
            println(world.cam)
        }

    }

    private val pointConsumer : DepthPointConsumer = zBuffer::addPoint

    override fun render(delta: Float) {

        if (worldChanged){
            zBuffer.invalidate()
            processFlow.process { obj ->
                obj.faces.forEach { face ->
                        drawFillFace(face, pointConsumer)
//                    drawStrokeFace(face, pointConsumer)
                } }
            worldChanged = false
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

        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            pitch += viewSensitivity
            updateCamFront()
        }

        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            pitch -= viewSensitivity
            updateCamFront()
        }

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            yaw -= viewSensitivity
            updateCamFront()
        }

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            yaw += viewSensitivity
            updateCamFront()
        }

        if (dragProcessor.isDragged()){
            yaw += (dragProcessor.getDeltaXAndReset()) * mouseSensitivity
            pitch += (dragProcessor.getDeltaYAndReset()) * mouseSensitivity

            updateCamFront()
        }
    }

    private fun updateCamFront() {
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