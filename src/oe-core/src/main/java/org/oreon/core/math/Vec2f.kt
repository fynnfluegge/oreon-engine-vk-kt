package org.oreon.core.math

class Vec2f {
    var x = 0f
    var y = 0f

    constructor() {
        x = 0f
        y = 0f
    }

    constructor(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    constructor(v: Vec2f) {
        x = v.x
        y = v.y
    }

    fun length(): Float {
        return Math.sqrt(x * x + y * y.toDouble()).toFloat()
    }

    fun dot(r: Vec2f): Float {
        return x * r.x + y * r.y
    }

    fun normalize(): Vec2f {
        val length = length()
        x /= length
        y /= length
        return this
    }

    fun add(r: Vec2f): Vec2f {
        return Vec2f(x + r.x, y + r.y)
    }

    fun add(r: Float): Vec2f {
        return Vec2f(x + r, y + r)
    }

    fun sub(r: Vec2f): Vec2f {
        return Vec2f(x - r.x, y - r.y)
    }

    fun sub(r: Float): Vec2f {
        return Vec2f(x - r, y - r)
    }

    fun mul(r: Vec2f): Vec2f {
        return Vec2f(x * r.x, y * r.y)
    }

    fun mul(r: Float): Vec2f {
        return Vec2f(x * r, y * r)
    }

    operator fun div(r: Vec2f): Vec2f {
        return Vec2f(x / r.x, y / r.y)
    }

    operator fun div(r: Float): Vec2f {
        return Vec2f(x / r, y / r)
    }

    override fun toString(): String {
        return "[$x,$y]"
    }
}