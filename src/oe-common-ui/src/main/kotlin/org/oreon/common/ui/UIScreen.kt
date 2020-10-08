package org.oreon.common.ui

import org.oreon.core.scenegraph.RenderList
import java.util.*
import java.util.function.Consumer

class UIScreen {

    val elements: ArrayList<UIElement> = ArrayList()
    fun render() {
        elements.forEach(Consumer { element: UIElement -> element.render() })
    }

    fun update() {
        elements.forEach(Consumer { element: UIElement -> element.update() })
    }

    fun record(renderList: RenderList?) {
        elements.forEach(Consumer { element: UIElement -> element.record(renderList) })
    }

    fun shutdown() {
        elements.forEach(Consumer { element: UIElement -> element.shutdown() })
    }
}