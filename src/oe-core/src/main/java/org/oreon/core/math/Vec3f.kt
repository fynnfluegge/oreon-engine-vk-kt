package org.oreon.core.math

class Vec3f {
    var x = 0f
    var y = 0f
    var z = 0f

    constructor() {
        x = 0f
        y = 0f
        z = 0f
    }

    constructor(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(x: Float) {
        this.x = x
        y = x
        z = x
    }

    constructor(v: Vec3f) {
        x = v.x
        y = v.y
        z = v.z
    }

    fun length(): Float {
        return Math.sqrt(x * x + y * y + (z * z).toDouble()).toFloat()
    }

    fun dot(r: Vec3f): Float {
        return x * r.x + y * r.y + z * r.z
    }

    fun cross(r: Vec3f): Vec3f {
        val x = y * r.z - z * r.y
        val y = z * r.x - this.x * r.z
        val z = this.x * r.y - this.y * r.x
        return Vec3f(x, y, z)
    }

    fun normalize(): Vec3f {
        val length = length()
        x /= length
        y /= length
        z /= length
        return this
    }

    fun rotate(angle: Float, axis: Vec3f): Vec3f {
        val sinHalfAngle = Math.sin(Math.toRadians(angle / 2.toDouble())).toFloat()
        val cosHalfAngle = Math.cos(Math.toRadians(angle / 2.toDouble())).toFloat()
        val rX = axis.x * sinHalfAngle
        val rY = axis.y * sinHalfAngle
        val rZ = axis.z * sinHalfAngle
        val rotation = Vec4f(rX, rY, rZ, cosHalfAngle)
        val conjugate = rotation.conjugate()
        val w = rotation.mul(this).mul(conjugate)
        x = w.x
        y = w.y
        z = w.z
        return this
    }

    fun add(r: Vec3f): Vec3f {
        return Vec3f(x + r.x, y + r.y, z + r.z)
    }

    fun add(r: Float): Vec3f {
        return Vec3f(x + r, y + r, z + r)
    }

    fun sub(r: Vec3f): Vec3f {
        return Vec3f(x - r.x, y - r.y, z - r.z)
    }

    fun sub(r: Float): Vec3f {
        return Vec3f(x - r, y - r, z - r)
    }

    fun mul(r: Vec3f): Vec3f {
        return Vec3f(x * r.x, y * r.y, z * r.z)
    }

    fun mul(x: Float, y: Float, z: Float): Vec3f {
        return Vec3f(this.x * x, this.y * y, this.z * z)
    }

    fun mul(r: Float): Vec3f {
        return Vec3f(x * r, y * r, z * r)
    }

    operator fun div(r: Vec3f): Vec3f {
        return Vec3f(x / r.x, y / r.y, z / r.z)
    }

    operator fun div(r: Float): Vec3f {
        return Vec3f(x / r, y / r, z / r)
    }

    fun abs(): Vec3f {
        return Vec3f(Math.abs(x), Math.abs(y), Math.abs(z))
    }

    fun equals(v: Vec3f): Boolean {
        return if (x == v.x && y == v.y && z == v.z) true else false
    }

    override fun toString(): String {
        return "[" + x + "," + y + "," + z + "]"
    }
}