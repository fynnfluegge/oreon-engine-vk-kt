package org.oreon.common.ui

import lombok.Getter
import lombok.Setter
import org.oreon.core.math.Matrix4f
import org.oreon.core.scenegraph.Renderable

@Getter
@Setter
abstract class UIElement(xPos: Int, yPos: Int, xScaling: Int, yScaling: Int) : Renderable() {
    protected var orthographicMatrix: Matrix4f? = null
    override fun update() {}
    open fun update(text: String?) {}

    init {
        setOrthographicMatrix(Matrix4f().Orthographic2D())
        worldTransform!!.setTranslation(xPos.toFloat(), yPos.toFloat(), 0f)
        worldTransform!!.setScaling(xScaling.toFloat(), yScaling.toFloat(), 0f)
        setOrthographicMatrix(getOrthographicMatrix().mul(worldTransform!!.worldMatrix))
    }
}