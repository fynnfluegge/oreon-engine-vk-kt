package org.oreon.common.terrain

import org.oreon.common.quadtree.Quadtree
import org.oreon.core.math.Transform
import org.oreon.core.math.Vec2f
import org.oreon.core.scenegraph.NodeComponent
import org.oreon.core.scenegraph.NodeComponentType

abstract class TerrainQuadtree(components: Map<NodeComponentType?, NodeComponent?>?,
                               rootChunkCount: Int, horizontalScaling: Float) : Quadtree() {
    init {
        val worldTransform = Transform()
        worldTransform.setTranslation(-0.5f * horizontalScaling, 0f, -0.5f * horizontalScaling)
        worldTransform.setScaling(horizontalScaling, 0f, horizontalScaling)
        for (i in 0 until rootChunkCount) {
            for (j in 0 until rootChunkCount) {
                addChild(createChildChunk(components, quadtreeCache, worldTransform,
                        Vec2f(1f * i / rootChunkCount.toFloat(), 1f * j / rootChunkCount.toFloat()),
                        0, Vec2f(i.toFloat(), j.toFloat())))
            }
        }
    }
}