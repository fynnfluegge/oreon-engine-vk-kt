package org.oreon.common.terrain

import org.oreon.common.quadtree.QuadtreeConfig
import org.oreon.core.math.Vec2f

object TerrainHelper {
    fun getTerrainHeight(config: QuadtreeConfig,
                         x: Float, z: Float): Float {
        var h = 0f
        var pos = Vec2f()
        pos.x = x
        pos.y = z
        pos = pos.add(config.horizontalScaling / 2f)
        pos = pos.div(config.horizontalScaling)
        val floor = Vec2f(Math.floor(pos.x.toDouble()).toInt().toFloat(), Math.floor(pos.y.toDouble()).toInt().toFloat())
        pos = pos.sub(floor)
        pos = pos.mul(config.heightmap?.metaData!!.width.toFloat())
        val x0 = Math.floor(pos.x.toDouble()).toInt()
        val x1 = x0 + 1
        val z0 = Math.floor(pos.y.toDouble()).toInt()
        val z1 = z0 + 1
        val h0 = config.heightmapDataBuffer?.get(config.heightmap?.metaData!!.width * z0 + x0)
        val h1 = config.heightmapDataBuffer?.get(config.heightmap?.metaData!!.width * z0 + x1)
        val h2 = config.heightmapDataBuffer?.get(config.heightmap?.metaData!!.width * z1 + x0)
        val h3 = config.heightmapDataBuffer?.get(config.heightmap?.metaData!!.width * z1 + x1)
        val percentU = pos.x - x0
        val percentV = pos.y - z0
        var dU: Float = 0f
        var dV: Float = 0f
        if (percentU > percentV) {   // bottom triangle
            if (h1 != null) {
                dU = h1 - h0!!
            }
            if (h3 != null) {
                dV = h3 - h1!!
            }
        } else {   // top triangle
            if (h3 != null) {
                dU = h3 - h2!!
            }
            if (h2 != null) {
                dV = h2 - h0!!
            }
        }
        if (h0 != null) {
            h = h0 + dU * percentU + dV * percentV
        }
        h *= config.verticalScaling
        return h
    }
}