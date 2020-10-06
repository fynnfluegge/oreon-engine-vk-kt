package org.oreon.core.math

import org.oreon.core.context.BaseContext.Companion.window

class Matrix4f {
    var m: Array<FloatArray>
    fun Zero(): Matrix4f {
        m[0][0] = 0f
        m[0][1] = 0f
        m[0][2] = 0f
        m[0][3] = 0f
        m[1][0] = 0f
        m[1][1] = 0f
        m[1][2] = 0f
        m[1][3] = 0f
        m[2][0] = 0f
        m[2][1] = 0f
        m[2][2] = 0f
        m[2][3] = 0f
        m[3][0] = 0f
        m[3][1] = 0f
        m[3][2] = 0f
        m[3][3] = 0f
        return this
    }

    fun Identity(): Matrix4f {
        m[0][0] = 1f
        m[0][1] = 0f
        m[0][2] = 0f
        m[0][3] = 0f
        m[1][0] = 0f
        m[1][1] = 1f
        m[1][2] = 0f
        m[1][3] = 0f
        m[2][0] = 0f
        m[2][1] = 0f
        m[2][2] = 1f
        m[2][3] = 0f
        m[3][0] = 0f
        m[3][1] = 0f
        m[3][2] = 0f
        m[3][3] = 1f
        return this
    }

    fun Orthographic2D(width: Int, height: Int): Matrix4f {
        m[0][0] = 2f / width.toFloat()
        m[0][1] = 0f
        m[0][2] = 0f
        m[0][3] = (-1).toFloat()
        m[1][0] = 0f
        m[1][1] = 2f / height.toFloat()
        m[1][2] = 0f
        m[1][3] = (-1).toFloat()
        m[2][0] = 0f
        m[2][1] = 0f
        m[2][2] = 1f
        m[2][3] = 0f
        m[3][0] = 0f
        m[3][1] = 0f
        m[3][2] = 0f
        m[3][3] = 1f
        return this
    }

    fun Orthographic2D(): Matrix4f {
        //Z-Value 1: depth of orthographic OOB between 0 and -1
        m[0][0] = 2f / window.width.toFloat()
        m[0][1] = 0f
        m[0][2] = 0f
        m[0][3] = (-1).toFloat()
        m[1][0] = 0f
        m[1][1] = 2f / window.height.toFloat()
        m[1][2] = 0f
        m[1][3] = (-1).toFloat()
        m[2][0] = 0f
        m[2][1] = 0f
        m[2][2] = 1f
        m[2][3] = 0f
        m[3][0] = 0f
        m[3][1] = 0f
        m[3][2] = 0f
        m[3][3] = 1f
        return this
    }

    fun Translation(translation: Vec3f): Matrix4f {
        m[0][0] = 1f
        m[0][1] = 0f
        m[0][2] = 0f
        m[0][3] = translation.x
        m[1][0] = 0f
        m[1][1] = 1f
        m[1][2] = 0f
        m[1][3] = translation.y
        m[2][0] = 0f
        m[2][1] = 0f
        m[2][2] = 1f
        m[2][3] = translation.z
        m[3][0] = 0f
        m[3][1] = 0f
        m[3][2] = 0f
        m[3][3] = 1f
        return this
    }

    fun Rotation(rotation: Vec3f): Matrix4f {
        val x = Math.toRadians(rotation.x.toDouble()).toFloat()
        val y = Math.toRadians(rotation.y.toDouble()).toFloat()
        val z = Math.toRadians(rotation.z.toDouble()).toFloat()
        val sinX = Math.sin(x.toDouble()).toFloat()
        val sinY = Math.sin(y.toDouble()).toFloat()
        val sinZ = Math.sin(z.toDouble()).toFloat()
        val cosX = Math.cos(x.toDouble()).toFloat()
        val cosY = Math.cos(y.toDouble()).toFloat()
        val cosZ = Math.cos(z.toDouble()).toFloat()
        val sinXsinY = sinX * sinY
        val cosXsinY = cosX * sinY
        m[0][0] = cosY * cosZ
        m[0][1] = cosY * sinZ
        m[0][2] = -sinY
        m[0][3] = 0f
        m[1][0] = sinXsinY * cosZ - cosX * sinZ
        m[1][1] = sinXsinY * sinZ + cosX * cosZ
        m[1][2] = sinX * cosY
        m[1][3] = 0f
        m[2][0] = cosXsinY * cosZ + sinX * sinZ
        m[2][1] = cosXsinY * sinZ - sinX * cosZ
        m[2][2] = cosX * cosY
        m[2][3] = 0f
        m[3][0] = 0f
        m[3][1] = 0f
        m[3][2] = 0f
        m[3][3] = 1f
        return this
    }

    fun Scaling(scaling: Vec3f): Matrix4f {
        m[0][0] = scaling.x
        m[0][1] = 0f
        m[0][2] = 0f
        m[0][3] = 0f
        m[1][0] = 0f
        m[1][1] = scaling.y
        m[1][2] = 0f
        m[1][3] = 0f
        m[2][0] = 0f
        m[2][1] = 0f
        m[2][2] = scaling.z
        m[2][3] = 0f
        m[3][0] = 0f
        m[3][1] = 0f
        m[3][2] = 0f
        m[3][3] = 1f
        return this
    }

    fun MakeTransform(translation: Vec3f, rotation: Vec3f, scaling: Vec3f): Matrix4f {
        // Convert euler angles to a quaternion
        val cr = Math.cos(rotation.x * 0.5).toFloat()
        val sr = Math.sin(rotation.x * 0.5).toFloat()
        val cp = Math.cos(rotation.y * 0.5).toFloat()
        val sp = Math.sin(rotation.y * 0.5).toFloat()
        val cy = Math.cos(rotation.z * 0.5).toFloat()
        val sy = Math.sin(rotation.z * 0.5).toFloat()
        val w = cy * cr * cp + sy * sr * sp
        val x = cy * sr * cp - sy * cr * sp
        val y = cy * cr * sp + sy * sr * cp
        val z = sy * cr * cp - cy * sr * sp

        // Cache some data for further computations
        val x2 = x + x
        val y2 = y + y
        val z2 = z + z
        val xx = x * x2
        val xy = x * y2
        val xz = x * z2
        val yy = y * y2
        val yz = y * z2
        val zz = z * z2
        val wx = w * x2
        val wy = w * y2
        val wz = w * z2
        val scalingX = scaling.x
        val scalingY = scaling.y
        val scalingZ = scaling.z

        // Apply rotation and scale simultaneously, simply adding the translation.
        m[0][0] = (1f - (yy + zz)) * scalingX
        m[0][1] = (xy + wz) * scalingX
        m[0][2] = (xz - wy) * scalingX
        m[0][3] = translation.x
        m[1][0] = (xy - wz) * scalingY
        m[1][1] = (1f - (xx + zz)) * scalingY
        m[1][2] = (yz + wx) * scalingY
        m[1][3] = translation.y
        m[2][0] = (xz + wy) * scalingZ
        m[2][1] = (yz - wx) * scalingZ
        m[2][2] = (1f - (xx + yy)) * scalingZ
        m[2][3] = translation.z
        m[3][0] = 0f
        m[3][1] = 0f
        m[3][2] = 0f
        m[3][3] = 1f
        return this
    }

    fun OrthographicProjection(l: Float, r: Float, b: Float, t: Float, n: Float, f: Float): Matrix4f {
        m[0][0] = 2.0f / (r - l)
        m[0][1] = 0f
        m[0][2] = 0f
        m[0][3] = -(r + l) / (r - l)
        m[1][0] = 0f
        m[1][1] = 2.0f / (t - b)
        m[1][2] = 0f
        m[1][3] = -(t + b) / (t - b)
        m[2][0] = 0f
        m[2][1] = 0f
        m[2][2] = 2.0f / (f - n)
        m[2][3] = -(f + n) / (f - n)
        m[3][0] = 0f
        m[3][1] = 0f
        m[3][2] = 0f
        m[3][3] = 1f
        return this
    }

    fun PerspectiveProjection(fovY: Float, width: Float, height: Float, zNear: Float, zFar: Float): Matrix4f {
        val tanFOV = Math.tan(Math.toRadians(fovY / 2.toDouble())).toFloat()
        val aspectRatio = width / height
        m[0][0] = 1 / (tanFOV * aspectRatio)
        m[0][1] = 0f
        m[0][2] = 0f
        m[0][3] = 0f
        m[1][0] = 0f
        m[1][1] = 1 / tanFOV
        m[1][2] = 0f
        m[1][3] = 0f
        m[2][0] = 0f
        m[2][1] = 0f
        m[2][2] = zFar / (zFar - zNear)
        m[2][3] = zFar * zNear / (zFar - zNear)
        m[3][0] = 0f
        m[3][1] = 0f
        m[3][2] = 1f
        m[3][3] = 1f
        return this
    }

    fun View(forward: Vec3f, up: Vec3f): Matrix4f {
        val r = up.cross(forward)
        m[0][0] = r.x
        m[0][1] = r.y
        m[0][2] = r.z
        m[0][3] = 0f
        m[1][0] = up.x
        m[1][1] = up.y
        m[1][2] = up.z
        m[1][3] = 0f
        m[2][0] = forward.x
        m[2][1] = forward.y
        m[2][2] = forward.z
        m[2][3] = 0f
        m[3][0] = 0f
        m[3][1] = 0f
        m[3][2] = 0f
        m[3][3] = 1f
        return this
    }

    fun mul(r: Matrix4f): Matrix4f {
        val res = Matrix4f()
        for (i in 0..3) {
            for (j in 0..3) {
                res[i, j] = m[i][0] * r[0, j] + m[i][1] * r[1, j] + m[i][2] * r[2, j] + m[i][3] * r[3, j]
            }
        }
        return res
    }

    fun mul(v: Vec4f): Vec4f {
        val res = Vec4f(0f, 0f, 0f, 0f)
        res.x = m[0][0] * v.x + m[0][1] * v.y + m[0][2] * v.z + m[0][3] * v.w
        res.y = m[1][0] * v.x + m[1][1] * v.y + m[1][2] * v.z + m[1][3] * v.w
        res.z = m[2][0] * v.x + m[2][1] * v.y + m[2][2] * v.z + m[2][3] * v.w
        res.w = m[3][0] * v.x + m[3][1] * v.y + m[3][2] * v.z + m[3][3] * v.w
        return res
    }

    fun transpose(): Matrix4f {
        val result = Matrix4f()
        for (i in 0..3) {
            for (j in 0..3) {
                result[i, j] = get(j, i)
            }
        }
        return result
    }

    fun invert(): Matrix4f {
        val s0 = get(0, 0) * get(1, 1) - get(1, 0) * get(0, 1)
        val s1 = get(0, 0) * get(1, 2) - get(1, 0) * get(0, 2)
        val s2 = get(0, 0) * get(1, 3) - get(1, 0) * get(0, 3)
        val s3 = get(0, 1) * get(1, 2) - get(1, 1) * get(0, 2)
        val s4 = get(0, 1) * get(1, 3) - get(1, 1) * get(0, 3)
        val s5 = get(0, 2) * get(1, 3) - get(1, 2) * get(0, 3)
        val c5 = get(2, 2) * get(3, 3) - get(3, 2) * get(2, 3)
        val c4 = get(2, 1) * get(3, 3) - get(3, 1) * get(2, 3)
        val c3 = get(2, 1) * get(3, 2) - get(3, 1) * get(2, 2)
        val c2 = get(2, 0) * get(3, 3) - get(3, 0) * get(2, 3)
        val c1 = get(2, 0) * get(3, 2) - get(3, 0) * get(2, 2)
        val c0 = get(2, 0) * get(3, 1) - get(3, 0) * get(2, 1)
        val div = s0 * c5 - s1 * c4 + s2 * c3 + s3 * c2 - s4 * c1 + s5 * c0
        if (div == 0f) System.err.println("not invertible")
        val invdet = 1.0f / div
        val invM = Matrix4f()
        invM[0, 0] = (get(1, 1) * c5 - get(1, 2) * c4 + get(1, 3) * c3) * invdet
        invM[0, 1] = (-get(0, 1) * c5 + get(0, 2) * c4 - get(0, 3) * c3) * invdet
        invM[0, 2] = (get(3, 1) * s5 - get(3, 2) * s4 + get(3, 3) * s3) * invdet
        invM[0, 3] = (-get(2, 1) * s5 + get(2, 2) * s4 - get(2, 3) * s3) * invdet
        invM[1, 0] = (-get(1, 0) * c5 + get(1, 2) * c2 - get(1, 3) * c1) * invdet
        invM[1, 1] = (get(0, 0) * c5 - get(0, 2) * c2 + get(0, 3) * c1) * invdet
        invM[1, 2] = (-get(3, 0) * s5 + get(3, 2) * s2 - get(3, 3) * s1) * invdet
        invM[1, 3] = (get(2, 0) * s5 - get(2, 2) * s2 + get(2, 3) * s1) * invdet
        invM[2, 0] = (get(1, 0) * c4 - get(1, 1) * c2 + get(1, 3) * c0) * invdet
        invM[2, 1] = (-get(0, 0) * c4 + get(0, 1) * c2 - get(0, 3) * c0) * invdet
        invM[2, 2] = (get(3, 0) * s4 - get(3, 1) * s2 + get(3, 3) * s0) * invdet
        invM[2, 3] = (-get(2, 0) * s4 + get(2, 1) * s2 - get(2, 3) * s0) * invdet
        invM[3, 0] = (-get(1, 0) * c3 + get(1, 1) * c1 - get(1, 2) * c0) * invdet
        invM[3, 1] = (get(0, 0) * c3 - get(0, 1) * c1 + get(0, 2) * c0) * invdet
        invM[3, 2] = (-get(3, 0) * s3 + get(3, 1) * s1 - get(3, 2) * s0) * invdet
        invM[3, 3] = (get(2, 0) * s3 - get(2, 1) * s1 + get(2, 2) * s0) * invdet
        return invM
    }

    val translation: Vec3f
        get() = Vec3f(m[0][3], m[1][3], m[2][3])

    fun equals(m: Matrix4f): Boolean {
        return this.m[0][0] == m.m[0][0] && this.m[0][1] == m.m[0][1] && this.m[0][2] == m.m[0][2] && this.m[0][3] == m.m[0][3] && this.m[1][0] == m.m[1][0] && this.m[1][1] == m.m[1][1] && this.m[1][2] == m.m[1][2] && this.m[1][3] == m.m[1][3] && this.m[2][0] == m.m[2][0] && this.m[2][1] == m.m[2][1] && this.m[2][2] == m.m[2][2] && this.m[2][3] == m.m[2][3] && this.m[3][0] == m.m[3][0] && this.m[3][1] == m.m[3][1] && this.m[3][2] == m.m[3][2] && this.m[3][3] == m.m[3][3]
    }

    operator fun set(x: Int, y: Int, value: Float) {
        m[x][y] = value
    }

    operator fun get(x: Int, y: Int): Float {
        return m[x][y]
    }

    override fun toString(): String {
        return """|${m[0][0]} ${m[0][1]} ${m[0][2]} ${m[0][3]}|
            |${m[1][0]} ${m[1][1]} ${m[1][2]} ${m[1][3]}|
            |${m[2][0]} ${m[2][1]} ${m[2][2]} ${m[2][3]}|
            |${m[3][0]} ${m[3][1]} ${m[3][2]} ${m[3][3]}|"""
    }

    init {
        m = Array(4) { FloatArray(4) }
    }
}