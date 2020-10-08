package org.oreon.core.scenegraph

abstract class NodeComponent : Cloneable {
    var parent: Renderable? = null
    fun update() {}
    fun input() {}
    fun render() {}
    open fun shutdown() {}
    @Throws(CloneNotSupportedException::class)
    public override fun clone(): NodeComponent {
        return super.clone() as NodeComponent
    }
}