package org.oreon.core.scenegraph

import org.oreon.core.math.Transform

class Scenegraph : Node() {
    val root: Node
    var terrain: Node
        private set
    var water: Node
        private set
    private val transparentObjects: Node
    private var hasTerrain = false
    private var hasWater = false
    override fun render() {
        root.render()
        terrain.render()
        water.render()
    }

    override fun renderWireframe() {
        root.renderWireframe()
        terrain.renderWireframe()
        water.renderWireframe()
    }

    fun renderTransparentObejcts() {
        transparentObjects.render()
    }

    override fun renderShadows() {
        root.renderShadows()
        terrain.renderShadows()
        water.renderShadows()
    }

    override fun record(renderList: RenderList?) {
        root.record(renderList)
        terrain.record(renderList)
        water.record(renderList)
    }

    fun recordTransparentObjects(renderList: RenderList?) {
        transparentObjects.record(renderList)
    }

    override fun update() {
        root.update()
        terrain.update()
        water.update()
        transparentObjects.update()
    }

    override fun updateLights() {
        root.updateLights()
    }

    override fun input() {
        root.input()
        terrain.input()
        water.input()
    }

    override fun shutdown() {
        root.shutdown()
        terrain.shutdown()
        water.shutdown()
        transparentObjects.shutdown()
    }

    fun addObject(`object`: Node?) {
        root.addChild(`object`!!)
    }

    fun addTransparentObject(`object`: Node?) {
        transparentObjects.addChild(`object`!!)
    }

    fun setTerrain(vTerrain: Node) {
        vTerrain.setParent(this)
        hasTerrain = true
        terrain = vTerrain
    }

    fun setWater(vWater: Node) {
        vWater.setParent(this)
        hasWater = true
        water = vWater
    }

    fun hasTerrain(): Boolean {
        return hasTerrain
    }

    fun hasWater(): Boolean {
        return hasWater
    }

    init {
        worldTransform = Transform()
        root = Node()
        terrain = Node()
        water = Node()
        transparentObjects = Node()
        root.setParent(this)
        terrain.setParent(this)
        water.setParent(this)
        transparentObjects.setParent(this)
    }
}