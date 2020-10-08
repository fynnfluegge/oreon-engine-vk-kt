package org.oreon.core.light

import org.oreon.core.context.BaseContext.Companion.window
import org.oreon.core.math.Vec2f
import org.oreon.core.math.Vec3f
import org.oreon.core.math.Vec4f
import org.oreon.core.query.OcclusionQuery
import org.oreon.core.scenegraph.Node

open class Light : Node {
    var color: Vec3f? = null
    var intensity = 0f
    var occlusionQuery: OcclusionQuery? = null

    constructor(color: Vec3f?, intensity: Float) {
        this.color = color
        this.intensity = intensity
    }

    constructor() {}

    val screenSpacePosition: Vec2f?
        get() {
            val clipSpacePos = localTransform!!.modelViewProjectionMatrix.mul(Vec4f(0f, 0f, 0f, 1f))
            val ndcSpacePos = Vec3f(clipSpacePos.x / clipSpacePos.w, clipSpacePos.y / clipSpacePos.w, clipSpacePos.z / clipSpacePos.w)
            return if (ndcSpacePos.x < -1 || ndcSpacePos.x > 1 || ndcSpacePos.y < -1 || ndcSpacePos.y > 1) {
                null
            } else Vec2f(ndcSpacePos.x, ndcSpacePos.y).add(1.0f).div(2.0f).mul(
                    Vec2f(window.width.toFloat(), window.height.toFloat()))
        }

    fun occlusionQuery() {
        occlusionQuery!!.doQuery(this)
    }
}