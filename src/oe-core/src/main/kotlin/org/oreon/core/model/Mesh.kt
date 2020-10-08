package org.oreon.core.model

import org.oreon.core.math.Vec2f
import org.oreon.core.model.Vertex.VertexLayout
import java.util.*

class Mesh(var vertices: Array<Vertex>, var indices: IntArray) {
    var vertexLayout: VertexLayout? = null
    var isTangentSpace = false
    val uvCoords: List<Vec2f>
        get() {
            val uvCoords = ArrayList<Vec2f>()
            for (v in vertices) {
                uvCoords.add(v.uvCoord)
            }
            return uvCoords
        }
}