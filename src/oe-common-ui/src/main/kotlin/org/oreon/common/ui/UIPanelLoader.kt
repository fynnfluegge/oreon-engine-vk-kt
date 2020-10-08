package org.oreon.common.ui

import org.oreon.core.math.Vec3f
import org.oreon.core.model.Mesh
import org.oreon.core.model.Vertex
import org.oreon.core.model.Vertex.VertexLayout
import org.oreon.core.util.Util.removeEmptyStrings
import org.oreon.core.util.Util.toIntArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

object UIPanelLoader {
    fun load(fileName: String): Mesh? {
        val splitArray = fileName.split("\\.".toRegex()).toTypedArray()
        val ext = splitArray[splitArray.size - 1]
        if (ext == "gui") {
            val vertices = ArrayList<Vertex>()
            val indices = ArrayList<Int>()
            var meshReader: BufferedReader? = null
            val `is` = UIPanelLoader::class.java.classLoader.getResourceAsStream(fileName)
            try {
                meshReader = BufferedReader(InputStreamReader(`is`))
                var line: String?
                while (meshReader.readLine().also { line = it } != null) {
                    var tokens = removeEmptyStrings(line!!.split(" ".toRegex()).toTypedArray())
                    if (tokens.size == 0 || tokens[0] == "#") continue
                    if (tokens[0] == "v") {
                        vertices.add(Vertex(Vec3f(java.lang.Float.valueOf(tokens[1]),
                                java.lang.Float.valueOf(tokens[2]),
                                java.lang.Float.valueOf(tokens[3]))))
                    } else if (tokens[0] == "f") {
                        indices.add(tokens[1]!!.toInt() - 1)
                        indices.add(tokens[2]!!.toInt() - 1)
                        indices.add(tokens[3]!!.toInt() - 1)
                    }
                }
                meshReader.close()
                val vertexData = vertices.toTypedArray()
                val objectArray = indices.toTypedArray()
                val indexData: IntArray = toIntArray(objectArray)
                val mesh = Mesh(vertexData, indexData)
                mesh.vertexLayout = VertexLayout.POS_UV
                return mesh
            } catch (e: Exception) {
                e.printStackTrace()
                System.exit(1)
            }
        } else {
            System.err.println("Error: wrong file format for mesh data: $ext")
            Exception().printStackTrace()
            System.exit(1)
        }
        return null
    }
}