package org.oreon.core.instanced

import org.oreon.core.math.Matrix4f
import org.oreon.core.math.Vec3f
import org.oreon.core.scenegraph.Node
import org.oreon.core.util.IntegerReference
import java.util.*

abstract class InstancedCluster : Node() {
    var worldMatrices: List<Matrix4f> = ArrayList()
    var modelMatrices: List<Matrix4f> = ArrayList()
    var highPolyIndices: List<Int> = ArrayList()
    var lowPolyIndices: List<Int> = ArrayList()
    var highPolyInstances: IntegerReference? = null
    var lowPolyInstances: IntegerReference? = null
    var center: Vec3f? = null
    fun updateUBOs() {}
    fun placeObject() {}
}