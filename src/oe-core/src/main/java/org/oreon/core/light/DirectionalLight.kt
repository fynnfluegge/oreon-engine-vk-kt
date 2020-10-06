package org.oreon.core.light

import org.lwjgl.glfw.GLFW
import org.oreon.core.context.BaseContext.Companion.camera
import org.oreon.core.context.BaseContext.Companion.config
import org.oreon.core.context.BaseContext.Companion.input
import org.oreon.core.math.Matrix4f
import org.oreon.core.math.Vec3f
import org.oreon.core.shadow.PssmCamera
import org.oreon.core.util.BufferUtil
import org.oreon.core.util.Constants
import java.nio.FloatBuffer

abstract class DirectionalLight private constructor(private var direction: Vec3f, ambient: Vec3f, color: Vec3f?, intensity: Float) : Light(color, intensity) {
    var ambient: Vec3f? = null
    var m_View: Matrix4f
    var right: Vec3f? = null
    var up: Vec3f
    val splitLightCameras: Array<PssmCamera?>
    var floatBufferLight: FloatBuffer? = null
    var floatBufferMatrices: FloatBuffer
    val lightBufferSize = java.lang.Float.BYTES * 12
    val matricesBufferSize = (java.lang.Float.BYTES * 16 * 7 // 6 matrices, 16 floats per matrix 
            + java.lang.Float.BYTES * 24 // 6 floats, 3 floats offset each
            )

    protected constructor() : this(config.sunPosition!!.normalize(),
            Vec3f(config.ambient),
            config.sunColor,
            config.sunIntensity) {
    }

    override fun update() {
        if (camera.isCameraRotated ||
                camera.isCameraMoved) {
            updateShadowMatrices(false)
            updateMatricesUbo()
        }


        // change sun orientation
        if (input.isKeyHolding(GLFW.GLFW_KEY_I)) {
            if (getDirection().y >= -0.8f) {
                setDirection(getDirection().add(Vec3f(0f, -0.001f, 0f)).normalize())
                updateLightBuffer()
                updateShadowMatrices(true)
                updateLightUbo()
                updateMatricesUbo()
            }
        }
        if (input.isKeyHolding(GLFW.GLFW_KEY_K)) {
            if (getDirection().y <= 0.00f) {
                setDirection(getDirection().add(Vec3f(0f, 0.001f, 0f)).normalize())
                updateLightBuffer()
                updateShadowMatrices(true)
                updateLightUbo()
                updateMatricesUbo()
            }
        }
        if (input.isKeyHolding(GLFW.GLFW_KEY_J)) {
            setDirection(getDirection().add(Vec3f(0.00075f, 0f, -0.00075f)).normalize())
            updateLightBuffer()
            updateShadowMatrices(true)
            updateLightUbo()
            updateMatricesUbo()
        }
        if (input.isKeyHolding(GLFW.GLFW_KEY_L)) {
            setDirection(getDirection().add(Vec3f(-0.00075f, 0f, 0.00075f)).normalize())
            updateLightBuffer()
            updateShadowMatrices(true)
            updateLightUbo()
            updateMatricesUbo()
        }
    }

    fun updateLightBuffer() {
        floatBufferLight!!.clear()
        floatBufferLight!!.put(BufferUtil.createFlippedBuffer(getDirection()))
        floatBufferLight!!.put(intensity)
        floatBufferLight!!.put(BufferUtil.createFlippedBuffer(ambient))
        floatBufferLight!!.put(0f)
        floatBufferLight!!.put(BufferUtil.createFlippedBuffer(color))
        floatBufferLight!!.put(0f)
        floatBufferLight!!.flip()
    }

    fun updateShadowMatrices(hasSunPositionChanged: Boolean) {
        floatBufferMatrices.clear()
        for (i in splitLightCameras.indices) {
            if (i == splitLightCameras.size - 1) {
                if (hasSunPositionChanged) {
                    splitLightCameras[i]!!.update(m_View, up, right)
                }
                floatBufferMatrices.put(BufferUtil.createFlippedBuffer(splitLightCameras[i]!!.m_orthographicViewProjection))
            } else {
                splitLightCameras[i]!!.update(m_View, up, right)
                floatBufferMatrices.put(BufferUtil.createFlippedBuffer(splitLightCameras[i]!!.m_orthographicViewProjection))
            }
        }
    }

    fun getDirection(): Vec3f {
        return direction
    }

    fun setDirection(direction: Vec3f) {
        this.direction = direction
        up = Vec3f(direction.x, 0f, direction.z)
        up.y = -(up.x * direction.x + up.z * direction.z) / direction.y
        if (direction.dot(up) != 0f) //			log.warn("DirectionalLight vector up " + up + " and direction " +  direction + " not orthogonal");
            right = up.cross(getDirection()).normalize()
        m_View = Matrix4f().View(getDirection(), up)
        config.sunPosition = getDirection()
    }

    abstract fun updateLightUbo()
    abstract fun updateMatricesUbo()

    init {
        this.ambient = ambient
        up = Vec3f(direction.x, 0f, direction.z)
        up.y = -(up.x * direction.x + up.z * direction.z) / direction.y
        if (direction.dot(up) != 0f) //			log.warn("DirectionalLight vector up " + up + " and direction " +  direction + " not orthogonal");
            right = up.cross(getDirection()).normalize()
        m_View = Matrix4f().View(getDirection(), up)
        floatBufferMatrices = BufferUtil.createFloatBuffer(matricesBufferSize)
        splitLightCameras = arrayOfNulls(Constants.PSSM_SPLITS)
        run {
            var i = 0
            while (i < Constants.PSSM_SPLITS * 2) {
                splitLightCameras[i / 2] = PssmCamera(Constants.PSSM_SPLIT_SHEME[i] * Constants.ZFAR,
                        Constants.PSSM_SPLIT_SHEME[i + 1] * Constants.ZFAR)
                splitLightCameras[i / 2]!!.update(m_View, up, right)
                floatBufferMatrices.put(BufferUtil.createFlippedBuffer(splitLightCameras[i / 2]!!.m_orthographicViewProjection))
                i += 2
            }
        }
        var i = 1
        while (i < Constants.PSSM_SPLITS * 2) {
            floatBufferMatrices.put(Constants.PSSM_SPLIT_SHEME[i])
            floatBufferMatrices.put(0f)
            floatBufferMatrices.put(0f)
            floatBufferMatrices.put(0f)
            i += 2
        }
        floatBufferLight = BufferUtil.createFloatBuffer(lightBufferSize)
        updateLightBuffer()
    }
}