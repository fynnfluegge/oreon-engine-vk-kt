package org.oreon.core.light

import java.util.*

object LightHandler {
    var lights: List<Light> = ArrayList()
    fun doOcclusionQueries() {
        for (light in lights) {
            light.occlusionQuery()
        }
    }
}