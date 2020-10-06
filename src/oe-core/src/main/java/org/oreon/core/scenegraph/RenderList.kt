package org.oreon.core.scenegraph

import java.util.*

open class RenderList {

    val objectList: LinkedHashMap<String, Renderable> = LinkedHashMap()
    var changed = false

    operator fun contains(id: String): Boolean {
        return objectList.containsKey(id)
    }

    operator fun get(key: String): Renderable? {
        return objectList[key]
    }

    fun add(`object`: Renderable) {
        objectList[`object`.id] = `object`
    }

    fun remove(`object`: Renderable) {
        objectList.remove(`object`.id)
    }

    fun remove(key: String) {
        objectList.remove(key)
    }

    val keySet: Set<String>
        get() = objectList.keys
    val entrySet: Set<Map.Entry<String, Renderable>>
        get() = objectList.entries
    val values: Collection<Renderable>
        get() = objectList.values

    fun sortFrontToBack(): List<Renderable>? {

        // TODO
        return null
    }

    fun sortBackToFront(): List<Renderable>? {

        // TODO
        return null
    }

    fun hasChanged(): Boolean {
        return changed
    }

    val isEmpty: Boolean
        get() = objectList.isEmpty()

}