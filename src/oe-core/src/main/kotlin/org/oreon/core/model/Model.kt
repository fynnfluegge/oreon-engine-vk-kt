package org.oreon.core.model

import org.oreon.core.scenegraph.NodeComponent

class Model : NodeComponent() {
    var mesh: Mesh? = null
    var material: Material? = null
}