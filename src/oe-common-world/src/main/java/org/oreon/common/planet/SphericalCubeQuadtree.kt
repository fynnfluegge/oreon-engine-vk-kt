package org.oreon.common.planet

import org.oreon.common.quadtree.Quadtree
import org.oreon.common.quadtree.QuadtreeConfig
import org.oreon.core.math.Transform
import org.oreon.core.math.Vec2f
import org.oreon.core.scenegraph.NodeComponent
import org.oreon.core.scenegraph.NodeComponentType

abstract class SphericalCubeQuadtree(components: Map<NodeComponentType, NodeComponent>,
                                     quadtreeConfig: QuadtreeConfig, rootChunkCount: Int, horizontalScaling: Float) : Quadtree() {
    init {

        // cube face 0 - front face
        // x-rotation 90 degrees, z-translation -1
        val worldTransformFace0 = Transform()
        worldTransformFace0.setTranslation(-0.5f * horizontalScaling,
                0.5f * horizontalScaling, -0.5f * horizontalScaling)
        worldTransformFace0.setRotation(-90f, 0f, 0f)
        worldTransformFace0.setScaling(horizontalScaling)
        for (i in 0 until rootChunkCount) {
            for (j in 0 until rootChunkCount) {
                val newChunk = createChildChunk(components,
                        quadtreeCache, worldTransformFace0,
                        Vec2f(1f * i / rootChunkCount.toFloat(), 1f * j / rootChunkCount.toFloat()),
                        0, Vec2f(i.toFloat(), j.toFloat()))
                addChild(newChunk)
            }
        }

        // cube face 1 - left face
        // x-rotation 90 degrees, y-rotation 90 degrees, x-translation -1
        val worldTransformFace1 = Transform()
        worldTransformFace1.setTranslation(-0.5f * horizontalScaling,
                0.5f * horizontalScaling, -0.5f * horizontalScaling)
        worldTransformFace1.setRotation(-90f, 90f, 0f)
        worldTransformFace1.setScaling(horizontalScaling)
        for (i in 0 until rootChunkCount) {
            for (j in 0 until rootChunkCount) {
                val newChunk = createChildChunk(components,
                        quadtreeCache, worldTransformFace1,
                        Vec2f(1f * i / rootChunkCount.toFloat(), 1f * j / rootChunkCount.toFloat()),
                        0, Vec2f(i.toFloat(), j.toFloat()))
                addChild(newChunk)
            }
        }

        // cube face 2 - back face
        // x-rotation 90 degrees, y-rotation 180 degrees, z-translation +1
        val worldTransformFace2 = Transform()
        worldTransformFace2.setTranslation(-0.5f * horizontalScaling,
                0.5f * horizontalScaling, -0.5f * horizontalScaling)
        worldTransformFace2.setRotation(-90f, 180f, 0f)
        worldTransformFace2.setScaling(horizontalScaling)
        for (i in 0 until rootChunkCount) {
            for (j in 0 until rootChunkCount) {
                val newChunk = createChildChunk(components,
                        quadtreeCache, worldTransformFace2,
                        Vec2f(1f * i / rootChunkCount.toFloat(), 1f * j / rootChunkCount.toFloat()),
                        0, Vec2f(i.toFloat(), j.toFloat()))
                addChild(newChunk)
            }
        }

        // cube face 3 - right face
        // x-rotation 90 degrees, y-rotation 270 degrees, x-translation +1
        val worldTransformFace3 = Transform()
        worldTransformFace3.setTranslation(-0.5f * horizontalScaling,
                0.5f * horizontalScaling, -0.5f * horizontalScaling)
        worldTransformFace3.setRotation(-90f, -90f, 0f)
        worldTransformFace3.setScaling(horizontalScaling)
        for (i in 0 until rootChunkCount) {
            for (j in 0 until rootChunkCount) {
                val newChunk = createChildChunk(components,
                        quadtreeCache, worldTransformFace3,
                        Vec2f(1f * i / rootChunkCount.toFloat(), 1f * j / rootChunkCount.toFloat()),
                        0, Vec2f(i.toFloat(), j.toFloat()))
                addChild(newChunk)
            }
        }

        // cube face 4 - top face
        // y-translation +1
        val worldTransformFace4 = Transform()
        worldTransformFace4.setTranslation(-0.5f * horizontalScaling,
                0.5f * horizontalScaling, -0.5f * horizontalScaling)
        worldTransformFace4.setScaling(horizontalScaling)
        for (i in 0 until rootChunkCount) {
            for (j in 0 until rootChunkCount) {
                val newChunk = createChildChunk(components,
                        quadtreeCache, worldTransformFace4,
                        Vec2f(1f * i / rootChunkCount.toFloat(), 1f * j / rootChunkCount.toFloat()),
                        0, Vec2f(i.toFloat(), j.toFloat()))
                addChild(newChunk)
            }
        }

        // cube face 5 - bottom face
        // y-translation -1
        val worldTransformFace5 = Transform()
        worldTransformFace5.setTranslation(-0.5f * horizontalScaling,
                -0.5f * horizontalScaling, -0.5f * horizontalScaling)
        worldTransformFace5.setScaling(horizontalScaling)
        for (i in 0 until rootChunkCount) {
            for (j in 0 until rootChunkCount) {
                val newChunk = createChildChunk(components,
                        quadtreeCache, worldTransformFace5,
                        Vec2f(1f * i / rootChunkCount.toFloat(), 1f * j / rootChunkCount.toFloat()),
                        0, Vec2f(i.toFloat(), j.toFloat()))
                addChild(newChunk)
            }
        }
    }
}