package org.oreon.vk.components.planet

import org.oreon.common.planet.SphericalCubeQuadtree
import org.oreon.common.quadtree.QuadtreeCache
import org.oreon.common.quadtree.QuadtreeConfig
import org.oreon.common.quadtree.QuadtreeNode
import org.oreon.core.math.Transform
import org.oreon.core.math.Vec2f
import org.oreon.core.scenegraph.NodeComponent
import org.oreon.core.scenegraph.NodeComponentType
import org.oreon.vk.components.terrain.TerrainChunk

class PlanetQuadtree(components: Map<NodeComponentType, NodeComponent>?,
                     vQuadtreeConfig: QuadtreeConfig?, rootChunkCount: Int, horizontalScaling: Float) : SphericalCubeQuadtree(components, vQuadtreeConfig, rootChunkCount, horizontalScaling) {
    override fun createChildChunk(components: Map<NodeComponentType, NodeComponent>, quadtreeCache: QuadtreeCache,
                                  worldTransform: Transform, location: Vec2f, levelOfDetail: Int, index: Vec2f): QuadtreeNode {
        return TerrainChunk(components, quadtreeCache, worldTransform, location, levelOfDetail, index)
    }
}