package com.bsuir.objviewer.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Scaling
import com.bsuir.objviewer.ObjViewer
import com.bsuir.objviewer.core.algorithm.DepthPointConsumer
import com.bsuir.objviewer.core.algorithm.drawFillFace
import com.bsuir.objviewer.core.algorithm.drawStrokeFace
import com.bsuir.objviewer.core.extension.cross
import com.bsuir.objviewer.core.extension.normalized
import com.bsuir.objviewer.core.model.Color
import com.bsuir.objviewer.core.model.World
import com.bsuir.objviewer.core.processflow.ProcessFlow
import com.bsuir.objviewer.core.textureprovider.ImageTextureProvider
import com.bsuir.objviewer.core.textureprovider.PlainColorTextureProvider
import com.bsuir.objviewer.core.textureprovider.TextureProvider
import com.bsuir.objviewer.core.zbuffer.ZBuffer
import ktx.app.KtxScreen
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.operations.minus
import org.jetbrains.kotlinx.multik.ndarray.operations.plus
import org.jetbrains.kotlinx.multik.ndarray.operations.times
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.sin


class CanvasScreen(private val application: ObjViewer, world: World) : KtxScreen {

    private val cam = world.cam
    private var mouseSensitivity = 0.5
    private val viewSensitivity = 5
    private var yaw = 0.0
    private var pitch = 0.0
    private val zBuffer = ZBuffer(
        3840,
        2160,
        Color.WHITE
    )

    private var worldChanged = true

//    private val textureProvider: TextureProvider = PlainColorTextureProvider(Color.GRAY)
    private val textureProvider: TextureProvider = ImageTextureProvider(ImageIO.read(File("./rigged_hand/textures/HAND_C.jpg")))

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
        }
        zBuffer.setSize(cam.windowSize)
    }

    private val pixmap = Pixmap(cam.windowSize.width, cam.windowSize.height, Pixmap.Format.RGBA8888)
    private val pixels = pixmap.pixels.asIntBuffer()

    private val pointConsumer : DepthPointConsumer = zBuffer::addPoint

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        val size = Scaling.fit.apply(cam.windowSize.width.toFloat(), cam.windowSize.height.toFloat(), width.toFloat(), height.toFloat())
        val viewportX = ((width - size.x) / 2).toInt()
        val viewportY = ((height - size.y) / 2).toInt()
        val viewportWidth = size.x.toInt()
        val viewportHeight = size.y.toInt()
        Gdx.gl.glViewport(viewportX, viewportY, viewportWidth, viewportHeight)
    }

    override fun render(delta: Float) {
        if (worldChanged){
            zBuffer.invalidate()
            processFlow.process { face ->
                drawFillFace(face, textureProvider, pointConsumer)
//                drawStrokeFace(face, pointConsumer, Color.BLACK)
            }
            pixels.clear()
            zBuffer.transferTo { x, y, color -> pixels.put(y * cam.windowSize.width + x, color.packed) }
            pixels.clear()

            worldChanged = false
        }

        val img = Texture(pixmap)

        application.batch.begin()
        application.batch.draw(img, 0f, 0f)
        application.batch.end()

        img.dispose()
        processInput()
    }

    override fun dispose() {
        super.dispose()
        pixmap.dispose()
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