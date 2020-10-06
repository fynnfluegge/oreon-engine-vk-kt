package org.oreon.core.math

class Vec4f {
    var x = 0f
    var y = 0f
    var z = 0f
    var w = 0f

    constructor(x: Float, y: Float, z: Float, w: Float) {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
    }

    constructor(v: Vec3f, w: Float) {
        x = v.x
        y = v.y
        z = v.z
        this.w = w
    }

    fun length(): Float {
        return Math.sqrt(x * x + y * y + z * z + (w * w).toDouble()).toFloat()
    }

    fun normalize(): Vec4f {
        val length = length()
        x /= length
        y /= length
        z /= length
        w /= length
        return this
    }

    fun conjugate(): Vec4f {
        return Vec4f(-x, -y, -z, w)
    }

    fun mul(r: Vec4f): Vec4f {
        val w_ = w * r.w - x * r.x - y * r.y - z * r.z
        val x_ = x * r.w + w * r.x + y * r.z - z * r.y
        val y_ = y * r.w + w * r.y + z * r.x - x * r.z
        val z_ = z * r.w + w * r.z + x * r.y - y * r.x
        return Vec4f(x_, y_, z_, w_)
    }

    fun mul(r: Vec3f): Vec4f {
        val w_ = -x * r.x - y * r.y - z * r.z
        val x_ = w * r.x + y * r.z - z * r.y
        val y_ = w * r.y + z * r.x - x * r.z
        val z_ = w * r.z + x * r.y - y * r.x
        return Vec4f(x_, y_, z_, w_)
    }

    operator fun div(r: Float): Vec4f {
        val w_ = w / r
        val x_ = x / r
        val y_ = y / r
        val z_ = z / r
        return Vec4f(x_, y_, z_, w_)
    }

    fun mul(r: Float): Vec4f {
        val w_ = w * r
        val x_ = x * r
        val y_ = y * r
        val z_ = z * r
        return Vec4f(x_, y_, z_, w_)
    }

    fun sub(r: Vec4f): Vec4f {
        val w_ = w - r.w
        val x_ = x - r.x
        val y_ = y - r.y
        val z_ = z - r.z
        return Vec4f(x_, y_, z_, w_)
    }

    fun add(r: Vec4f): Vec4f {
        val w_ = w + r.w
        val x_ = x + r.x
        val y_ = y + r.y
        val z_ = z + r.z
        return Vec4f(x_, y_, z_, w_)
    }

    fun xyz(): Vec3f {
        return Vec3f(x, y, z)
    }

    override fun toString(): String {
        return "[" + x + "," + y + "," + z + "," + w + "]"
    }
}