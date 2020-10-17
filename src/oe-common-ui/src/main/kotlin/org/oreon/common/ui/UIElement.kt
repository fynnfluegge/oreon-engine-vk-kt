package org.oreon.common.ui

import org.oreon.core.math.Matrix4f
import org.oreon.core.scenegraph.Renderable

abstract class UIElement(xPos: Int, yPos: Int, xScaling: Int, yScaling: Int) : Renderable() {
    protected var orthographicMatrix: Matrix4f = Matrix4f().Orthographic2D()
    override fun update() {}
    open fun update(text: String?) {}

    init {
        worldTransform!!.setTranslation(xPos.toFloat(), yPos.toFloat(), 0f)
        worldTransform!!.setScaling(xScaling.toFloat(), yScaling.toFloat(), 0f)
        orthographicMatrix = orthographicMatrix!!.mul(worldTransform!!.worldMatrix)
    }
}