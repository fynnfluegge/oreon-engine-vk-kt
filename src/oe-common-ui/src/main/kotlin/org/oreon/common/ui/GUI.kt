package org.oreon.common.ui

import org.oreon.core.scenegraph.RenderList
import java.util.*
import java.util.function.Consumer

abstract class GUI {

    val screens = ArrayList<UIScreen>()
    open fun update() {
        screens.forEach(Consumer { screen: UIScreen -> screen.update() })
    }

    open fun render() {
        screens.forEach(Consumer { screen: UIScreen -> screen.render() })
    }

    fun record(renderList: RenderList?) {
        screens.forEach(Consumer { screen: UIScreen -> screen.record(renderList) })
    }

    open fun shutdown() {
        screens.forEach(Consumer { screen: UIScreen -> screen.shutdown() })
    }
}