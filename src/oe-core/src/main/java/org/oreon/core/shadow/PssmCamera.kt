package org.oreon.core.shadow

import org.oreon.core.context.BaseContext.Companion.camera
import org.oreon.core.math.Matrix4f
import org.oreon.core.math.Vec3f
import org.oreon.core.math.Vec4f

class PssmCamera(private val zNear: Float, private val zFar: Float) {
    private val frustumCorners: Array<Vec3f?> = arrayOfNulls(8)
    lateinit var m_orthographicViewProjection: Matrix4f
        private set

    fun update(m_View: Matrix4f, v_Up: Vec3f, v_Right: Vec3f) {
        updateFrustumCorners()
        updateOrthoMatrix(m_View, v_Up, v_Right)
    }

    private fun updateOrthoMatrix(m_View: Matrix4f, v_Up: Vec3f, v_Right: Vec3f) {
        frustumCorners[0] = m_View.mul(Vec4f(frustumCorners[0]!!, 1f)).xyz()
        frustumCorners[1] = m_View.mul(Vec4f(frustumCorners[1]!!, 1f)).xyz()
        frustumCorners[2] = m_View.mul(Vec4f(frustumCorners[2]!!, 1f)).xyz()
        frustumCorners[3] = m_View.mul(Vec4f(frustumCorners[3]!!, 1f)).xyz()
        frustumCorners[4] = m_View.mul(Vec4f(frustumCorners[4]!!, 1f)).xyz()
        frustumCorners[5] = m_View.mul(Vec4f(frustumCorners[5]!!, 1f)).xyz()
        frustumCorners[6] = m_View.mul(Vec4f(frustumCorners[6]!!, 1f)).xyz()
        frustumCorners[7] = m_View.mul(Vec4f(frustumCorners[7]!!, 1f)).xyz()
        val boundaries = getBoundaries(frustumCorners)
        val left = boundaries[0]
        val right = boundaries[1]
        val bottom = boundaries[2]
        val top = boundaries[3]
        val near = boundaries[4]
        val far = boundaries[5]
        m_orthographicViewProjection = Matrix4f().OrthographicProjection(left, right, bottom, top, near, far).mul(m_View)
    }

    fun getBoundaries(frustumCorners: Array<Vec3f?>): FloatArray {
        var xMin = 1000000f
        var xMax = -1000000f
        var yMin = 1000000f
        var yMax = -1000000f
        var zMin = 1000000f
        var zMax = -1000000f
        for (corner in frustumCorners) {
            if (corner!!.x < xMin) {
                xMin = corner.x
            }
            if (corner.x > xMax) {
                xMax = corner.x
            }
            if (corner.y < yMin) {
                yMin = corner.y
            }
            if (corner.y > yMax) {
                yMax = corner.y
            }
            if (corner.z < zMin) {
                zMin = corner.z
            }
            if (corner.z > zMax) {
                zMax = corner.z
            }
        }
        return floatArrayOf(xMin, xMax, yMin, yMax, zMin, zMax)
    }

    private fun updateFrustumCorners() {
        val right = camera.up.cross(camera.forward)
        val tanFOV = Math.tan(Math.toRadians(camera.fovY / 2.toDouble())).toFloat()
        val aspectRatio = camera.width / camera.height

        //width and height of near plane
        val heightNear = 2 * tanFOV * zNear
        val widthNear = heightNear * aspectRatio

        //width and height of far plane
        val heightFar = 2 * tanFOV * zFar
        val widthFar = heightFar * aspectRatio

        //center of planes
        val centerNear = camera.position.add(camera.forward.mul(zNear))
        val centerFar = camera.position.add(camera.forward.mul(zFar))
        val NearTopLeft = centerNear.add(camera.up.mul(heightNear / 2f).sub(right.mul(widthNear / 2f)))
        val NearTopRight = centerNear.add(camera.up.mul(heightNear / 2f).add(right.mul(widthNear / 2f)))
        val NearBottomLeft = centerNear.sub(camera.up.mul(heightNear / 2f).sub(right.mul(widthNear / 2f)))
        val NearBottomRight = centerNear.sub(camera.up.mul(heightNear / 2f).add(right.mul(widthNear / 2f)))
        val FarTopLeft = centerFar.add(camera.up.mul(heightFar / 2f).sub(right.mul(widthFar / 2f)))
        val FarTopRight = centerFar.add(camera.up.mul(heightFar / 2f).add(right.mul(widthFar / 2f)))
        val FarBottomLeft = centerFar.sub(camera.up.mul(heightFar / 2f).sub(right.mul(widthFar / 2f)))
        val FarBottomRight = centerFar.sub(camera.up.mul(heightFar / 2f).add(right.mul(widthFar / 2f)))
        frustumCorners[0] = NearTopLeft
        frustumCorners[1] = NearTopRight
        frustumCorners[2] = NearBottomLeft
        frustumCorners[3] = NearBottomRight
        frustumCorners[4] = FarTopLeft
        frustumCorners[5] = FarTopRight
        frustumCorners[6] = FarBottomLeft
        frustumCorners[7] = FarBottomRight
    }

}