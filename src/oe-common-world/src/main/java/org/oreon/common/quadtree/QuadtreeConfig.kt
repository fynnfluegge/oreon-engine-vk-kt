package org.oreon.common.quadtree

import org.oreon.core.image.Image
import org.oreon.core.model.Material
import org.oreon.core.scenegraph.NodeComponent
import java.io.IOException
import java.nio.FloatBuffer
import java.util.*

class QuadtreeConfig : NodeComponent() {
    var verticalScaling: Float
    var horizontalScaling: Float
    var bezier = 0
    var reflectionOffset = 0
    var uvScaling: Float
    var tessellationFactor: Int
    var tessellationSlope: Float
    var tessellationShift: Float
    var highDetailRange: Int
    var normalStrength = 0f
    var heightStrength = 0f
    var heightmap: Image? = null
    var normalmap: Image? = null
    var ambientmap: Image? = null
    var splatmap: Image? = null
    var heightmapDataBuffer: FloatBuffer? = null
    var heightmapResolution: Int
    var edgeElevation: Boolean
    var materials: List<Material> = ArrayList()
    var rootChunkCount: Int
    var lodCount: Int
    var lod_range = IntArray(8)
    var lod_morphing_area = IntArray(8)
    var diamond_square: Boolean

    fun setLodRange(index: Int, lod_range: Int) {
        this.lod_range[index] = lod_range
        lod_morphing_area[index] = lod_range - getMorphingArea4Lod(index + 1)
    }

    private fun getMorphingArea4Lod(lod: Int): Int {
        return (horizontalScaling / rootChunkCount / Math.pow(2.0, lod.toDouble())).toInt()
    }

    init {
        val properties = Properties()
        try {
            val stream = QuadtreeConfig::class.java.classLoader
                    .getResourceAsStream("terrain-config.properties")
            properties.load(stream)
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        verticalScaling = java.lang.Float.valueOf(properties.getProperty("scaling.y"))
        horizontalScaling = java.lang.Float.valueOf(properties.getProperty("scaling.xz"))
        rootChunkCount = Integer.valueOf(properties.getProperty("rootchunks.count"))
        tessellationFactor = Integer.valueOf(properties.getProperty("tessellationFactor"))
        tessellationSlope = java.lang.Float.valueOf(properties.getProperty("tessellationSlope"))
        tessellationShift = java.lang.Float.valueOf(properties.getProperty("tessellationShift"))
        uvScaling = java.lang.Float.valueOf(properties.getProperty("scaling.uv"))
        highDetailRange = Integer.valueOf(properties.getProperty("highDetail.range"))
        diamond_square = if (Integer.valueOf(properties.getProperty("diamond_square")) == 1) true else false
        heightmapResolution = Integer.valueOf(properties.getProperty("heightmap.resolution"))
        edgeElevation = if (Integer.valueOf(properties.getProperty("edge.elevation")) == 1) true else false
        lodCount = Integer.valueOf(properties.getProperty("lod.count"))
        for (i in 0 until lodCount) {
            if (Integer.valueOf(properties.getProperty("lodRanges.lod$i")) == 0) {
                lod_range[i] = 0
                lod_morphing_area[i] = 0
            } else {
                setLodRange(i, Integer.valueOf(properties.getProperty("lodRanges.lod$i")))
            }
        }
    }
}