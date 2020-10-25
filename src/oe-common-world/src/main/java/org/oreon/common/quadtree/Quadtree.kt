package org.oreon.common.quadtree

import org.oreon.core.context.BaseContext.Companion.camera
import org.oreon.core.math.Transform
import org.oreon.core.math.Vec2f
import org.oreon.core.scenegraph.Node
import org.oreon.core.scenegraph.NodeComponent
import org.oreon.core.scenegraph.NodeComponentType
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

abstract class Quadtree : Node(), Runnable {
    private val thread: Thread
    private val startUpdateQuadtreeLock: Lock
    private val startUpdateQuadtreeCondition: Condition
    private var isRunning = false
    private var updateCounter = 0
    private val updateThreshold = 2
    protected var quadtreeCache: QuadtreeCache

    fun updateQuadtree() {
        if (camera.isCameraMoved) {
            updateCounter++
        }
        if (updateCounter == updateThreshold) {
            for (node in getChildren()!!) {
                (node as QuadtreeNode).updateQuadtree()
            }
            updateCounter = 0
        }
    }

    fun start() {
        thread.start()
    }

    override fun run() {
        isRunning = true
        while (isRunning) {
            startUpdateQuadtreeLock.lock()
            try {
                startUpdateQuadtreeCondition.await()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                startUpdateQuadtreeLock.unlock()
            }
            updateQuadtree()
        }
    }

    fun signal() {
        startUpdateQuadtreeLock.lock()
        try {
            startUpdateQuadtreeCondition.signal()
        } finally {
            startUpdateQuadtreeLock.unlock()
        }
    }

    override fun shutdown() {
        isRunning = false
    }

    override fun update() {}
    abstract fun createChildChunk(components: Map<NodeComponentType, NodeComponent>,
                                  quadtreeCache: QuadtreeCache, worldTransform: Transform,
                                  location: Vec2f, levelOfDetail: Int, index: Vec2f): QuadtreeNode

    init {
        startUpdateQuadtreeLock = ReentrantLock()
        startUpdateQuadtreeCondition = startUpdateQuadtreeLock.newCondition()
        thread = Thread(this)
        quadtreeCache = QuadtreeCache()
    }
}