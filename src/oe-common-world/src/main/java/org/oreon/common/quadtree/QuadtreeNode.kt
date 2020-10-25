package org.oreon.common.quadtree

import lombok.Getter
import org.oreon.core.context.BaseContext.Companion.camera
import org.oreon.core.context.BaseContext.Companion.config
import org.oreon.core.math.Transform
import org.oreon.core.math.Vec2f
import org.oreon.core.math.Vec3f
import org.oreon.core.scenegraph.NodeComponent
import org.oreon.core.scenegraph.NodeComponentType
import org.oreon.core.scenegraph.RenderList
import org.oreon.core.scenegraph.Renderable

abstract class QuadtreeNode(components: Map<NodeComponentType?, NodeComponent>,
                            quadtreeCache: QuadtreeCache, worldTransform: Transform,
                            location: Vec2f, lod: Int, index: Vec2f?) : Renderable() {
    protected var worldPos: Vec3f? = null
    protected var isleaf: Boolean
    protected var quadtreeCache: QuadtreeCache
    protected var quadtreeConfig: QuadtreeConfig?

    @Getter
    protected var chunkConfig: ChunkConfig
    override fun update() {
        for (child in getChildren()!!) child.update()
    }

    override fun render() {
        var renderChunk = false
        renderChunk = if (config.renderReflection || config.renderRefraction) {
            // render only first two lod's for reflection/refraction
            isleaf && chunkConfig.lod == 0 || !isleaf && chunkConfig.lod == 0 // || (!isleaf && lod == 1);
        } else {
            isleaf
        }
        if (renderChunk) {
            getComponents()[NodeComponentType.MAIN_RENDERINFO]!!.render()
        } else {
            for (child in getChildren()!!) child.render()
        }
    }

    override fun renderWireframe() {
        if (isleaf) {
            if (getComponents().containsKey(NodeComponentType.WIREFRAME_RENDERINFO)) {
                getComponents()[NodeComponentType.WIREFRAME_RENDERINFO]!!.render()
            }
        } else {
            for (child in getChildren()!!) child.renderWireframe()
        }
    }

    override fun renderShadows() {
        if (isleaf) {
            if (getComponents().containsKey(NodeComponentType.SHADOW_RENDERINFO)) {
                getComponents()[NodeComponentType.SHADOW_RENDERINFO]!!.render()
            }
        }
        for (child in getChildren()!!) child.renderShadows()
    }

    override fun record(renderList: RenderList?) {
        if (!renderList!!.contains(id)) {
            renderList.add(this)
            renderList.changed = true
        }
    }

    fun updateQuadtree() {
        updateChildNodes()
        for (node in getChildren()!!) {
            (node as QuadtreeNode).updateQuadtree()
        }
    }

    private fun updateChildNodes() {
        val distance = camera.position.sub(worldPos!!).length()
        if (distance < quadtreeConfig!!.lod_range[chunkConfig.lod]) {
            add4ChildNodes(chunkConfig.lod + 1)
        } else if (distance >= quadtreeConfig!!.lod_range[chunkConfig.lod]) {
            removeChildNodes()
        }
    }

    private fun add4ChildNodes(lod: Int) {
        if (isleaf) {
            isleaf = false
        }
        if (getChildren()!!.size == 0) {
            for (i in 0..1) {
                for (j in 0..1) {
                    addChild(createChildChunk(getComponents(), quadtreeCache, worldTransform,
                            chunkConfig.location!!.add(Vec2f(i * chunkConfig.gap / 2f, j * chunkConfig.gap / 2f)), lod, Vec2f(i.toFloat(), j.toFloat()))!!)
                }
            }
        }
    }

    private fun removeChildNodes() {
        if (!isleaf) {
            isleaf = true
        }
        if (getChildren()!!.size != 0) {
            getChildren().clear()
        }
    }

    // LOD|LOC_X|LOC_Y|INDEX_X|INDEX_Y|WORLDPOS
    val quadtreeCacheKey: String
        get() =// LOD|LOC_X|LOC_Y|INDEX_X|INDEX_Y|WORLDPOS
            (java.lang.String.valueOf(chunkConfig.lod) + java.lang.String.valueOf(chunkConfig.location!!.x) + java.lang.String.valueOf(chunkConfig.location!!.y)
                    + java.lang.String.valueOf(chunkConfig.index!!.x) + java.lang.String.valueOf(chunkConfig.index!!.y) + worldPos.toString())

    fun getQuadtreeCacheKey(lod: Int, location: Vec2f, index: Vec2f): String {

        // LOD|LOC_X|LOC_Y|INDEX_X|INDEX_Y
        return (lod.toString() + java.lang.String.valueOf(location.x) + java.lang.String.valueOf(location.y)
                + java.lang.String.valueOf(index.x) + java.lang.String.valueOf(index.y) + worldPos.toString())
    }

    fun cacheChildrenTree() {

        // traverse children tree until max LOD
        if (chunkConfig.lod < quadtreeConfig!!.lodCount) {
            if (getChildren()!!.size != 0) {
                for (child in getChildren()!!) {
                    quadtreeCache.addChunk(child)
                    (child as QuadtreeNode).cacheChildrenTree()
                }
                getChildren().clear()
            }
        }
    }

    abstract fun createChildChunk(components: Map<NodeComponentType?, NodeComponent?>?,
                                  quadtreeCache: QuadtreeCache?, worldTransform: Transform?, location: Vec2f?, levelOfDetail: Int, index: Vec2f?): QuadtreeNode?

    protected fun computeWorldPos() {

        // TODO here with matrix multiplication
        val loc = chunkConfig.location!!.add(chunkConfig.gap / 2f).mul(quadtreeConfig!!.horizontalScaling).sub(quadtreeConfig!!.horizontalScaling / 2f)
        val height = getTerrainHeight(loc.x, loc.y)
        worldPos = Vec3f(loc.x, height, loc.y)
    }

    private fun getTerrainHeight(x: Float, z: Float): Float {
        var h = 0f
        var pos = Vec2f()
        pos.x = x
        pos.y = z
        pos = pos.add(quadtreeConfig!!.horizontalScaling / 2f)
        pos = pos.div(quadtreeConfig!!.horizontalScaling)
        val floor = Vec2f(Math.floor(pos.x.toDouble()).toInt(), Math.floor(pos.y.toDouble()).toInt())
        pos = pos.sub(floor)
        pos = pos.mul(quadtreeConfig!!.heightmap!!.metaData!!.width.toFloat())
        val x0 = Math.floor(pos.x.toDouble()).toInt()
        val x1 = x0 + 1
        val z0 = Math.floor(pos.y.toDouble()).toInt()
        val z1 = z0 + 1
        val h0 = quadtreeConfig!!.heightmapDataBuffer!![quadtreeConfig!!.heightmap!!.metaData!!.width * z0 + x0]
        val h1 = quadtreeConfig!!.heightmapDataBuffer!![quadtreeConfig!!.heightmap!!.metaData!!.width * z0 + x1]
        val h2 = quadtreeConfig!!.heightmapDataBuffer!![quadtreeConfig!!.heightmap!!.metaData!!.width * z1 + x0]
        val h3 = quadtreeConfig!!.heightmapDataBuffer!![quadtreeConfig!!.heightmap!!.metaData!!.width * z1 + x1]
        val percentU = pos.x - x0
        val percentV = pos.y - z0
        val dU: Float
        val dV: Float
        if (percentU > percentV) {   // bottom triangle
            dU = h1 - h0
            dV = h3 - h1
        } else {   // top triangle
            dU = h3 - h2
            dV = h2 - h0
        }
        h = h0 + dU * percentU + dV * percentV
        h *= quadtreeConfig!!.verticalScaling
        return h
    }

    val quadtreeParent: QuadtreeNode
        get() = parentNode as QuadtreeNode

    init {
        try {
            addComponent(NodeComponentType.MAIN_RENDERINFO, components[NodeComponentType.MAIN_RENDERINFO]!!.clone())
            addComponent(NodeComponentType.WIREFRAME_RENDERINFO, components[NodeComponentType.WIREFRAME_RENDERINFO]!!.clone())
            if (components.containsKey(NodeComponentType.SHADOW_RENDERINFO)) {
                addComponent(NodeComponentType.SHADOW_RENDERINFO, components[NodeComponentType.SHADOW_RENDERINFO]!!.clone())
            }
            addComponent(NodeComponentType.CONFIGURATION, components[NodeComponentType.CONFIGURATION]!!)
        } catch (e: CloneNotSupportedException) {
            e.printStackTrace()
        }
        quadtreeConfig = getComponent(NodeComponentType.CONFIGURATION)
        chunkConfig = ChunkConfig(lod, location, index,
                1f / (quadtreeConfig!!.rootChunkCount * Math.pow(2.0, lod.toDouble()).toFloat()))
        this.quadtreeCache = quadtreeCache
        isleaf = true
        val localScaling = Vec3f(chunkConfig.gap, 0, chunkConfig.gap)
        val localTranslation = Vec3f(location.x, 0, location.y)
        localTransform!!.scaling = localScaling
        localTransform!!.translation = localTranslation
        worldTransform = worldTransform
        worldTransform.scaling!!.y = quadtreeConfig!!.verticalScaling
        computeWorldPos()
        updateQuadtree()
    }
}