package org.oreon.core.scenegraph

import org.oreon.core.math.Transform
import java.util.ArrayList
import java.util.UUID
import java.util.function.Consumer

open class Node {
    var id: String
        protected set
    var parentNode: Node? = null
        private set
    private var children: MutableList<Node>? = null
    var worldTransform: Transform? = null
    var localTransform: Transform? = null

    fun addChild(child: Node) {
        child.setParent(this)
        children!!.add(child)
    }

    open fun update() {
        worldTransform!!.rotation = parentNode!!.worldTransform!!.rotation?.let { worldTransform!!.localRotation!!.add(it) }
        worldTransform!!.translation = parentNode!!.worldTransform!!.translation?.let { worldTransform!!.localTranslation!!.add(it) }
        worldTransform!!.scaling = parentNode!!.worldTransform!!.scaling?.let { worldTransform!!.localScaling!!.mul(it) }
        for (child in children!!) child.update()
    }

    open fun updateLights() {
        for (child in children!!) child.updateLights()
    }

    open fun input() {
        children!!.forEach(Consumer { child: Node -> child.input() })
    }

    open fun render() {
        children!!.forEach(Consumer { child: Node -> child.render() })
    }

    open fun renderWireframe() {
        children!!.forEach(Consumer { child: Node -> child.renderWireframe() })
    }

    open fun renderShadows() {
        children!!.forEach(Consumer { child: Node -> child.renderShadows() })
    }

    open fun record(renderList: RenderList?) {
        children!!.forEach(Consumer { child: Node -> child.record(renderList) })
    }

    open fun shutdown() {
        children!!.forEach(Consumer { child: Node -> child.shutdown() })
    }

    fun <T> getParentObject(): T? {
        return parentNode as T?
    }

    fun setParent(parent: Node?) {
        parentNode = parent
    }

    fun getChildren(): List<Node>? {
        return children
    }

    fun setChildren(children: MutableList<Node>?) {
        this.children = children
    }

    init {
        id = UUID.randomUUID().toString()
        worldTransform = Transform()
        localTransform = Transform()
        setChildren(ArrayList())
    }
}