package org.oreon.core.math

import org.oreon.core.context.BaseContext.Companion.camera

class Transform {
    var translation: Vec3f? = null
    var rotation: Vec3f? = null
    var scaling: Vec3f? = null
    var localTranslation: Vec3f? = null
    var localRotation: Vec3f? = null
    var localScaling: Vec3f? = null
    val worldMatrix: Matrix4f
        get() {
            val matrix4f = Matrix4f()
            return matrix4f.MakeTransform(translation!!, rotation!!, scaling!!)
        }
    val worldMatrixRTS: Matrix4f
        get() {
            val translationMatrix = Matrix4f().Translation(translation!!)
            val rotationMatrix = Matrix4f().Rotation(rotation!!)
            val scalingMatrix = Matrix4f().Scaling(scaling!!)
            return rotationMatrix.mul(translationMatrix.mul(scalingMatrix))
        }
    val worldMatrixSRT: Matrix4f
        get() {
            val translationMatrix = Matrix4f().Translation(translation!!)
            val rotationMatrix = Matrix4f().Rotation(rotation!!)
            val scalingMatrix = Matrix4f().Scaling(scaling!!)
            return scalingMatrix.mul(rotationMatrix.mul(translationMatrix))
        }
    val modelMatrix: Matrix4f
        get() = Matrix4f().Rotation(rotation!!)
    val modelViewProjectionMatrix: Matrix4f
        get() = camera.viewProjectionMatrix.mul(worldMatrix)
    val originModelViewProjectionMatrix: Matrix4f
        get() = camera.originViewProjectionMatrix.mul(worldMatrix)
    val xzOriginModelViewProjectionMatrix: Matrix4f
        get() = camera.xzOriginViewProjectionMatrix.mul(worldMatrix)

    fun setTranslation(x: Float, y: Float, z: Float) {
        translation = Vec3f(x, y, z)
    }

    fun setRotation(x: Float, y: Float, z: Float) {
        rotation = Vec3f(x, y, z)
    }

    fun setScaling(x: Float, y: Float, z: Float) {
        scaling = Vec3f(x, y, z)
    }

    fun setScaling(s: Float) {
        scaling = Vec3f(s, s, s)
    }

    fun setLocalTranslation(x: Float, y: Float, z: Float) {
        localTranslation = Vec3f(x, y, z)
    }

    fun setLocalRotation(x: Float, y: Float, z: Float) {
        localRotation = Vec3f(x, y, z)
    }

    fun setLocalScaling(x: Float, y: Float, z: Float) {
        localScaling = Vec3f(x, y, z)
    }

    init {
        translation = Vec3f(0f, 0f, 0f)
        rotation = Vec3f(0f, 0f, 0f)
        scaling = Vec3f(1f, 1f, 1f)
        localTranslation = Vec3f(0f, 0f, 0f)
        localRotation = Vec3f(0f, 0f, 0f)
        localScaling = Vec3f(1f, 1f, 1f)
    }
}