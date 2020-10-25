package org.oreon.core.scenegraph

import java.util.*
import java.util.function.Consumer

open class Renderable : Node() {

    private val components: HashMap<NodeComponentType, NodeComponent> = HashMap()
    protected var render = true

    fun addComponent(type: NodeComponentType, component: NodeComponent) {
        component.parent = this
        components[type] = component
    }

    override fun update() {
        for ((key, value) in components) {
            if (key != NodeComponentType.LIGHT) {
                value.update()
            }
        }
        super.update()
    }

    override fun updateLights() {
        for ((key, value) in components) {
            if (key == NodeComponentType.LIGHT) {
                value.update()
            }
        }
        super.update()
    }

    override fun input() {
        components.values.forEach(Consumer { component: NodeComponent -> component.input() })
        super.input()
    }

    override fun render() {
        if (components.containsKey(NodeComponentType.MAIN_RENDERINFO)) {
            components[NodeComponentType.MAIN_RENDERINFO]!!.render()
        }
        super.render()
    }

    override fun renderWireframe() {
        if (components.containsKey(NodeComponentType.WIREFRAME_RENDERINFO)) {
            components[NodeComponentType.WIREFRAME_RENDERINFO]!!.render()
        }
        super.renderWireframe()
    }

    override fun renderShadows() {
        if (components.containsKey(NodeComponentType.SHADOW_RENDERINFO)) {
            components[NodeComponentType.SHADOW_RENDERINFO]!!.render()
        }
        super.renderShadows()
    }

    override fun record(renderList: RenderList?) {
        if (render) {
            if (!renderList!!.contains(id)) {
                renderList!!.add(this)
                renderList!!.changed = true;
            }
        } else {
            if (renderList!!.contains(id)) {
                renderList!!.remove(this)
                renderList!!.changed = true;
            }
        }
        super.record(renderList)
    }

    override fun shutdown() {
        components.values.forEach(Consumer { component: NodeComponent -> component.shutdown() })
        super.shutdown()
    }

    fun getComponents(): Map<NodeComponentType, NodeComponent> {
        return components
    }

    fun <T> getComponent(type: NodeComponentType): T {
        return components[type] as T
    }

}