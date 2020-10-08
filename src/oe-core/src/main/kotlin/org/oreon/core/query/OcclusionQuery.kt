package org.oreon.core.query

import org.oreon.core.light.Light
import org.oreon.core.scenegraph.Renderable
import java.nio.IntBuffer

abstract class OcclusionQuery {
    var id = 0
    var buffer: IntBuffer? = null
    var occlusionFactor = 0
    abstract fun doQuery(`object`: Renderable?)
    abstract fun doQuery(light: Light?)
    abstract fun delete()
}