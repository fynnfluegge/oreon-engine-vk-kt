package org.oreon.core.model

import org.oreon.core.image.Image
import org.oreon.core.math.Vec3f
import org.oreon.core.scenegraph.NodeComponent

class Material : NodeComponent() {
    private val name: String? = null
    private val diffusemap: Image? = null
    private val normalmap: Image? = null
    private val heightmap: Image? = null
    private val ambientmap: Image? = null
    private val specularmap: Image? = null
    private val alphamap: Image? = null
    private val color: Vec3f? = null
    private val heightScaling = 0f
    private val horizontalScaling = 0f
    private val emission = 0f
    private val shininess = 0f
}