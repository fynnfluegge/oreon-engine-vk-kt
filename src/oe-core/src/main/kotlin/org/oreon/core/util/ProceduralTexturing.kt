package org.oreon.core.util

import org.oreon.core.math.Vec2f
import org.oreon.core.model.Mesh
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

object ProceduralTexturing {
    fun sphere(mesh: Mesh) {
        for (i in mesh.vertices.indices) {
            if (mesh.vertices[i].uvCoord.x != 0.001f) mesh.vertices[i].uvCoord.x = (0.5 + Math.atan2(mesh.vertices[i].position.z.toDouble(), mesh.vertices[i].position.x.toDouble()) / (2 * Math.PI)).toFloat()
            mesh.vertices[i].uvCoord.y = (0.5 - Math.asin(mesh.vertices[i].position.y.toDouble()) / Math.PI).toFloat()
        }
    }

    fun dome(mesh: Mesh) {
        for (i in mesh.vertices.indices) {
            mesh.vertices[i].uvCoord = Vec2f((mesh.vertices[i].position.x + 1) * 0.5f, (mesh.vertices[i].position.z + 1) * 0.5f)
        }
    }

    fun noiseTexture() {
        val resolution = 512
        val image = BufferedImage(resolution, resolution, BufferedImage.TYPE_INT_RGB)
        val raster = image.raster
        val output = File("./" + "Noise512_3" + ".jpg")
        val rnd = Random()
        for (i in 0 until resolution) {
            for (j in 0 until resolution) {
                val noise = rnd.nextInt(Int.MAX_VALUE)
                raster.setSample(j, i, 0, noise)
                raster.setSample(j, i, 1, noise)
                raster.setSample(j, i, 2, noise)
            }
        }
        try {
            ImageIO.write(image, "jpg", output)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}