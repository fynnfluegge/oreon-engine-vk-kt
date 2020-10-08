package org.oreon.core.scenegraph

import org.lwjgl.glfw.GLFW
import org.oreon.core.CoreEngine.Companion.currentFrameTime
import org.oreon.core.context.BaseContext
import org.oreon.core.context.BaseContext.Companion.config
import org.oreon.core.context.BaseContext.Companion.window
import org.oreon.core.math.Matrix4f
import org.oreon.core.math.Vec3f
import org.oreon.core.math.Vec4f
import org.oreon.core.platform.Input
import org.oreon.core.util.BufferUtil
import org.oreon.core.util.Constants
import org.oreon.core.util.Util.normalizePlane
import java.nio.FloatBuffer

abstract class Camera protected constructor(position: Vec3f, forward: Vec3f, up: Vec3f) {
    private val yAxis = Vec3f(0f, 1f, 0f)
    private val input: Input
    var position: Vec3f
    var previousPosition: Vec3f
    var forward: Vec3f
    lateinit var previousForward: Vec3f
    var up: Vec3f
    var movAmt = 8.0f
    var rotAmt = 12.0f
    lateinit var projectionMatrix: Matrix4f
    var viewMatrix: Matrix4f
    var viewProjectionMatrix: Matrix4f
    var originViewMatrix: Matrix4f
    var originViewProjectionMatrix: Matrix4f
    var xzOriginViewMatrix: Matrix4f
    lateinit var xzOriginViewProjectionMatrix: Matrix4f
    var previousViewMatrix: Matrix4f
    var previousViewProjectionMatrix: Matrix4f
    var isCameraMoved = false
    var isCameraRotated = false
    var width: Float
        private set
    var height: Float
        private set
    var fovY = 0f
        private set
    var rotYstride = 0f
    var rotYamt = 0f
    var rotXstride = 0f
    var rotXamt = 0f
    var mouseSensitivity = 0.04f
    var isUpRotation = false
    var isDownRotation = false
    var isLeftRotation = false
    var isRightRotation = false
    val frustumPlanes = arrayOfNulls<Vec4f>(6)
    val frustumCorners = arrayOfNulls<Vec3f>(8)
    protected var floatBuffer: FloatBuffer
    protected val bufferSize = java.lang.Float.BYTES * (4 + 16 + 16 + 6 * 4 + 4)
    abstract fun init()
    abstract fun shutdown()
    open fun update() {
        previousPosition = Vec3f(position)
        previousForward = Vec3f(forward)
        isCameraMoved = false
        isCameraRotated = false
        movAmt += input.scrollOffset / 2
        movAmt = Math.max(0.1f, movAmt)
        if (input.isKeyHolding(GLFW.GLFW_KEY_W)) move(forward, movAmt)
        if (input.isKeyHolding(GLFW.GLFW_KEY_S)) move(forward, -movAmt)
        if (input.isKeyHolding(GLFW.GLFW_KEY_A)) move(left, movAmt)
        if (input.isKeyHolding(GLFW.GLFW_KEY_D)) move(right, movAmt)
        if (input.isKeyHolding(GLFW.GLFW_KEY_UP)) rotateX(-rotAmt / 8f)
        if (input.isKeyHolding(GLFW.GLFW_KEY_DOWN)) rotateX(rotAmt / 8f)
        if (input.isKeyHolding(GLFW.GLFW_KEY_LEFT)) rotateY(-rotAmt / 8f)
        if (input.isKeyHolding(GLFW.GLFW_KEY_RIGHT)) rotateY(rotAmt / 8f)

        // free mouse rotation
        if (input.isButtonHolding(0) || input.isButtonHolding(2)) {
            var dy = input.lockedCursorPosition.y - input.cursorPosition.y
            var dx = input.lockedCursorPosition.x - input.cursorPosition.x
            if (Math.abs(dy) < 1) dy = 0f
            if (Math.abs(dx) < 1) dx = 0f

            // y-axxis rotation
            if (dy != 0f) {
                rotYamt = rotYamt - dy
                rotYstride = Math.abs(rotYamt * currentFrameTime * 10)
            }
            if (rotYamt != 0f || rotYstride != 0f) {

                // up-rotation
                if (rotYamt < 0) {
                    isUpRotation = true
                    isDownRotation = false
                    rotateX(-rotYstride * mouseSensitivity)
                    rotYamt += rotYstride
                    if (rotYamt > 0) rotYamt = 0f
                }
                // down-rotation
                if (rotYamt > 0) {
                    isUpRotation = false
                    isDownRotation = true
                    rotateX(rotYstride * mouseSensitivity)
                    rotYamt -= rotYstride
                    if (rotYamt < 0) rotYamt = 0f
                }
                // smooth-stop
                if (rotYamt == 0f) {
                    rotYstride *= 0.85f
                    if (isUpRotation) rotateX(-rotYstride * mouseSensitivity)
                    if (isDownRotation) rotateX(rotYstride * mouseSensitivity)
                    if (rotYstride < 0.001f) rotYstride = 0f
                }
            }

            // x-axxis rotation
            if (dx != 0f) {
                rotXamt += dx
                rotXstride = Math.abs(rotXamt * currentFrameTime * 10)
            }
            if (rotXamt != 0f || rotXstride != 0f) {

                // right-rotation
                if (rotXamt < 0) {
                    isRightRotation = true
                    isLeftRotation = false
                    rotateY(rotXstride * mouseSensitivity)
                    rotXamt += rotXstride
                    if (rotXamt > 0) rotXamt = 0f
                }
                // left-rotation
                if (rotXamt > 0) {
                    isRightRotation = false
                    isLeftRotation = true
                    rotateY(-rotXstride * mouseSensitivity)
                    rotXamt -= rotXstride
                    if (rotXamt < 0) rotXamt = 0f
                }
                // smooth-stop
                if (rotXamt == 0f) {
                    rotXstride *= 0.85f
                    if (isRightRotation) rotateY(rotXstride * mouseSensitivity)
                    if (isLeftRotation) rotateY(-rotXstride * mouseSensitivity)
                    if (rotXstride < 0.001f) rotXstride = 0f
                }
            }
            GLFW.glfwSetCursorPos(window.id,
                    input.lockedCursorPosition.x.toDouble(),
                    input.lockedCursorPosition.y.toDouble())
        }
        if (!position.equals(previousPosition)) {
            isCameraMoved = true
        }
        if (!forward.equals(previousForward)) {
            isCameraRotated = true
        }
        previousViewMatrix = viewMatrix
        previousViewProjectionMatrix = viewProjectionMatrix
        val vOriginViewMatrix = Matrix4f().View(forward, up)
        viewMatrix = vOriginViewMatrix.mul(Matrix4f().Translation(position.mul(-1f)))
        originViewMatrix = vOriginViewMatrix
        xzOriginViewMatrix = vOriginViewMatrix.mul(
                Matrix4f().Translation(
                        Vec3f(0f, position.y, 0f).mul(-1f)))
        viewProjectionMatrix = projectionMatrix.mul(viewMatrix)
        originViewProjectionMatrix = projectionMatrix.mul(originViewMatrix)
        xzOriginViewProjectionMatrix = projectionMatrix.mul(xzOriginViewMatrix)
        floatBuffer.clear()
        floatBuffer.put(BufferUtil.createFlippedBuffer(position))
        floatBuffer.put(0f)
        floatBuffer.put(BufferUtil.createFlippedBuffer(viewMatrix))
        floatBuffer.put(BufferUtil.createFlippedBuffer(viewProjectionMatrix))
        floatBuffer.put(BufferUtil.createFlippedBuffer(frustumPlanes))
        floatBuffer.put(width)
        floatBuffer.put(height)
        floatBuffer.put(0f)
        floatBuffer.put(0f)
        floatBuffer.flip()
    }

    fun move(dir: Vec3f?, amount: Float) {
        val newPos = position.add(dir!!.mul(amount))
        position = newPos
    }

    private fun initfrustumPlanes() {
        // ax * bx * cx +  d = 0; store a,b,c,d

        //left plane
        val leftPlane = Vec4f(
                projectionMatrix[3, 0] + projectionMatrix[0, 0]
                        * (Math.tan(Math.toRadians(fovY / 2.toDouble()))
                        * (config.frameWidth.toDouble()
                        / config.frameHeight.toDouble())).toFloat(),
                projectionMatrix[3, 1] + projectionMatrix[0, 1],
                projectionMatrix[3, 2] + projectionMatrix[0, 2],
                projectionMatrix[3, 3] + projectionMatrix[0, 3])
        frustumPlanes[0] = normalizePlane(leftPlane)

        //right plane
        val rightPlane = Vec4f(
                projectionMatrix[3, 0] - projectionMatrix[0, 0]
                        * (Math.tan(Math.toRadians(fovY / 2.toDouble()))
                        * (config.frameWidth.toDouble()
                        / config.frameHeight.toDouble())).toFloat(),
                projectionMatrix[3, 1] - projectionMatrix[0, 1],
                projectionMatrix[3, 2] - projectionMatrix[0, 2],
                projectionMatrix[3, 3] - projectionMatrix[0, 3])
        frustumPlanes[1] = normalizePlane(rightPlane)

        //bot plane
        val botPlane = Vec4f(
                projectionMatrix[3, 0] + projectionMatrix[1, 0],
                projectionMatrix[3, 1] + projectionMatrix[1, 1]
                        * Math.tan(Math.toRadians(fovY / 2.toDouble())).toFloat(),
                projectionMatrix[3, 2] + projectionMatrix[1, 2],
                projectionMatrix[3, 3] + projectionMatrix[1, 3])
        frustumPlanes[2] = normalizePlane(botPlane)

        //top plane
        val topPlane = Vec4f(
                projectionMatrix[3, 0] - projectionMatrix[1, 0],
                projectionMatrix[3, 1] - projectionMatrix[1, 1]
                        * Math.tan(Math.toRadians(fovY / 2.toDouble())).toFloat(),
                projectionMatrix[3, 2] - projectionMatrix[1, 2],
                projectionMatrix[3, 3] - projectionMatrix[1, 3])
        frustumPlanes[3] = normalizePlane(topPlane)

        //near plane
        val nearPlane = Vec4f(
                projectionMatrix[3, 0] + projectionMatrix[2, 0],
                projectionMatrix[3, 1] + projectionMatrix[2, 1],
                projectionMatrix[3, 2] + projectionMatrix[2, 2],
                projectionMatrix[3, 3] + projectionMatrix[2, 3])
        frustumPlanes[4] = normalizePlane(nearPlane)

        //far plane
        val farPlane = Vec4f(
                projectionMatrix[3, 0] - projectionMatrix[2, 0],
                projectionMatrix[3, 1] - projectionMatrix[2, 1],
                projectionMatrix[3, 2] - projectionMatrix[2, 2],
                projectionMatrix[3, 3] - projectionMatrix[2, 3])
        frustumPlanes[5] = normalizePlane(farPlane)
    }

    fun rotateY(angle: Float) {
        var hAxis = yAxis.cross(forward).normalize()
        forward.rotate(angle, yAxis).normalize()
        up = forward.cross(hAxis).normalize()

        // this is for align y-axxis of camera vectors
        // there is a kind of numeric bug, when camera is rotating very fast, camera vectors skewing
        hAxis = yAxis.cross(forward).normalize()
        forward.rotate(0f, yAxis).normalize()
        up = forward.cross(hAxis).normalize()
    }

    fun rotateX(angle: Float) {
        val hAxis = yAxis.cross(forward).normalize()
        forward.rotate(angle, hAxis).normalize()
        up = forward.cross(hAxis).normalize()
    }

    val left: Vec3f
        get() {
            val left = forward.cross(up)
            left.normalize()
            return left
        }
    val right: Vec3f
        get() {
            val right = up.cross(forward)
            right.normalize()
            return right
        }

    fun setProjection(fovY: Float, width: Float, height: Float) {
        this.fovY = fovY
        this.width = width
        this.height = height
        projectionMatrix = Matrix4f().PerspectiveProjection(
                fovY, width, height, Constants.ZNEAR, Constants.ZFAR)
    }

    init {
        width = config.frameWidth.toFloat()
        height = config.frameHeight.toFloat()
        this.position = position
        this.forward = forward.normalize()
        this.up = up.normalize()
        setProjection(70f, width, height)
        viewMatrix = Matrix4f().View(forward, up).mul(
                Matrix4f().Translation(position.mul(-1f)))
        originViewMatrix = Matrix4f().View(forward, up).mul(
                Matrix4f().Identity())
        xzOriginViewMatrix = Matrix4f().View(forward, up).mul(
                Matrix4f().Translation(
                        Vec3f(0f, position.y, 0f).mul(-1f)))
        initfrustumPlanes()
        previousViewMatrix = Matrix4f().Zero()
        previousViewProjectionMatrix = Matrix4f().Zero()
        previousPosition = Vec3f(0f, 0f, 0f)
        floatBuffer = BufferUtil.createFloatBuffer(bufferSize)
        viewProjectionMatrix = projectionMatrix.mul(viewMatrix)
        originViewProjectionMatrix = projectionMatrix.mul(originViewMatrix)
        input = BaseContext.input
    }
}