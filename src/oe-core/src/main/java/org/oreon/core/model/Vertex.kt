package org.oreon.core.model

import org.oreon.core.math.Vec2f
import org.oreon.core.math.Vec3f

class Vertex {
    lateinit var position: Vec3f
    lateinit var normal: Vec3f
    lateinit var uvCoord: Vec2f
    var tangent: Vec3f? = null
    var bitangent: Vec3f? = null

    enum class VertexLayout {
        POS_NORMAL_UV_TAN_BITAN, POS_NORMAL, POS_UV, POS, POS_NORMAL_UV, POS2D, POS2D_UV
    }

    constructor() {}
    constructor(pos: Vec3f) {
        position = pos
        uvCoord = Vec2f(0f, 0f)
        normal = Vec3f(0f, 0f, 0f)
    }

    constructor(pos: Vec3f, texture: Vec2f) {
        position = pos
        uvCoord = texture
        normal = Vec3f(0f, 0f, 0f)
    }

    companion object {
        const val BYTES = 14 * java.lang.Float.BYTES
        const val FLOATS = 14
    }
}