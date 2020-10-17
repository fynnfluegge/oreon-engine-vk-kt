package org.oreon.core.util

import org.oreon.core.image.ImageMetaData
import org.oreon.core.math.Vec2f
import org.oreon.core.math.Vec3f
import org.oreon.core.math.Vec4f
import org.oreon.core.model.Mesh
import org.oreon.core.model.Vertex
import java.nio.FloatBuffer
import kotlin.collections.ArrayList
import kotlin.math.ln

object Util {
    fun removeEmptyStrings(data: Array<String>): Array<String?> {
        val result = ArrayList<String>()
        for (i in data.indices) if (data[i] != "") result.add(data[i])
        val res = arrayOfNulls<String>(result.size)
        result.toArray(res)
        return res
    }

    fun toIntArray(data: Array<Int>): IntArray {
        val result = IntArray(data.size)
        for (i in data.indices) result[i] = data[i]
        return result
    }

    fun toIntArray(data: List<Int>): IntArray {
        val result = IntArray(data.size)
        for (i in data.indices) result[i] = data[i]
        return result
    }

    fun toVertexArray(data: FloatBuffer): Array<Vertex?> {
        val vertices = arrayOfNulls<Vertex>(data.limit() / Vertex.FLOATS)
        for (i in vertices.indices) {
            vertices[i] = Vertex()
            vertices[i]!!.position = Vec3f(data.get(), data.get(), data.get())
            vertices[i]!!.uvCoord = Vec2f(data.get(), data.get())
            vertices[i]!!.normal = Vec3f(data.get(), data.get(), data.get())
        }
        return vertices
    }

    fun toVertexArray(data: List<Vertex>): Array<Vertex> {
        val vertices = ArrayList<Vertex>()
        data.forEach { vertex ->
            val v = Vertex()
            v.position = vertex.position
            v.uvCoord = vertex.uvCoord
            v.normal = vertex.normal
            v.tangent = vertex.tangent
            v.bitangent = vertex.bitangent
            vertices.add(v)
        }
        return vertices.toTypedArray()
    }

    fun generateNormalsCW(vertices: Array<Vertex>, indices: IntArray) {
        run {
            var i = 0
            while (i < indices.size) {
                val v0 = vertices[indices[i]].position
                val v1 = vertices[indices[i + 1]].position
                val v2 = vertices[indices[i + 2]].position
                val normal = v1.sub(v0).cross(v2.sub(v0)).normalize()
                vertices[indices[i]].normal = vertices[indices[i]].normal.add(normal)
                vertices[indices[i + 1]].normal = vertices[indices[i + 1]].normal.add(normal)
                vertices[indices[i + 2]].normal = vertices[indices[i + 2]].normal.add(normal)
                i += 3
            }
        }
        for (i in vertices.indices) {
            vertices[i].normal = vertices[i].normal.normalize()
        }
    }

    fun generateNormalsCCW(vertices: Array<Vertex>, indices: IntArray) {
        run {
            var i = 0
            while (i < indices.size) {
                val v0 = vertices[indices[i]].position
                val v1 = vertices[indices[i + 1]].position
                val v2 = vertices[indices[i + 2]].position
                val normal = v2.sub(v0).cross(v1.sub(v0)).normalize()
                vertices[indices[i]].normal = vertices[indices[i]].normal.add(normal)
                vertices[indices[i + 1]].normal = vertices[indices[i + 1]].normal.add(normal)
                vertices[indices[i + 2]].normal = vertices[indices[i + 2]].normal.add(normal)
                i += 3
            }
        }
        for (i in vertices.indices) {
            vertices[i].normal = vertices[i].normal.normalize()
        }
    }

    fun generateNormalsCW(vertices: ArrayList<Vertex>, indices: ArrayList<Int?>) {
        run {
            var i = 0
            while (i < indices.size) {
                val v0 = vertices[indices[i]!!].position
                val v1 = vertices[indices[i + 1]!!].position
                val v2 = vertices[indices[i + 2]!!].position
                val normal = v1.sub(v0).cross(v2.sub(v0)).normalize()
                vertices[indices[i]!!].normal = vertices[indices[i]!!].normal.add(normal)
                vertices[indices[i + 1]!!].normal = vertices[indices[i + 1]!!].normal.add(normal)
                vertices[indices[i + 2]!!].normal = vertices[indices[i + 2]!!].normal.add(normal)
                i += 3
            }
        }
        for (i in vertices.indices) {
            vertices[i].normal = vertices[i].normal.normalize()
        }
    }

    fun generateNormalsCCW(vertices: ArrayList<Vertex>, indices: ArrayList<Int?>) {
        run {
            var i = 0
            while (i < indices.size) {
                val v0 = vertices[indices[i]!!].position
                val v1 = vertices[indices[i + 1]!!].position
                val v2 = vertices[indices[i + 2]!!].position
                val normal = v2.sub(v0).cross(v1.sub(v0)).normalize()
                vertices[indices[i]!!].normal = vertices[indices[i]!!].normal.add(normal)
                vertices[indices[i + 1]!!].normal = vertices[indices[i + 1]!!].normal.add(normal)
                vertices[indices[i + 2]!!].normal = vertices[indices[i + 2]!!].normal.add(normal)
                i += 3
            }
        }
        for (i in vertices.indices) {
            vertices[i].normal = vertices[i].normal.normalize()
        }
    }

    fun generateTangentsBitangents(mesh: Mesh) {
        var i = 0
        while (i < mesh.indices.size) {
            val v0 = mesh.vertices[mesh.indices[i]].position
            val v1 = mesh.vertices[mesh.indices[i + 1]].position
            val v2 = mesh.vertices[mesh.indices[i + 2]].position
            val uv0 = mesh.vertices[mesh.indices[i]].uvCoord
            val uv1 = mesh.vertices[mesh.indices[i + 1]].uvCoord
            val uv2 = mesh.vertices[mesh.indices[i + 2]].uvCoord
            val e1 = v1.sub(v0)
            val e2 = v2.sub(v0)
            val deltaUV1 = uv1.sub(uv0)
            val deltaUV2 = uv2.sub(uv0)
            val r = 1.0f / (deltaUV1.x * deltaUV2.y - deltaUV1.y * deltaUV2.x)
            var tangent = Vec3f()
            tangent.x = r * deltaUV2.y * e1.x - deltaUV1.y * e2.x
            tangent.y = r * deltaUV2.y * e1.y - deltaUV1.y * e2.y
            tangent.z = r * deltaUV2.y * e1.z - deltaUV1.y * e2.z
            var bitangent = Vec3f()
            var normal = mesh.vertices[mesh.indices[i]].normal.add(
                    mesh.vertices[mesh.indices[i + 1]].normal).add(
                    mesh.vertices[mesh.indices[i + 2]].normal)
            normal = normal.normalize()
            bitangent = tangent.cross(normal)
            tangent = tangent.normalize()
            bitangent = bitangent.normalize()
            if (mesh.vertices[mesh.indices[i]].tangent == null) mesh.vertices[mesh.indices[i]].tangent = Vec3f(0f, 0f, 0f)
            if (mesh.vertices[mesh.indices[i]].bitangent == null) mesh.vertices[mesh.indices[i]].bitangent = Vec3f(0f, 0f, 0f)
            if (mesh.vertices[mesh.indices[i + 1]].tangent == null) mesh.vertices[mesh.indices[i + 1]].tangent = Vec3f(0f, 0f, 0f)
            if (mesh.vertices[mesh.indices[i + 1]].bitangent == null) mesh.vertices[mesh.indices[i + 1]].bitangent = Vec3f(0f, 0f, 0f)
            if (mesh.vertices[mesh.indices[i + 2]].tangent == null) mesh.vertices[mesh.indices[i + 2]].tangent = Vec3f(0f, 0f, 0f)
            if (mesh.vertices[mesh.indices[i + 2]].bitangent == null) mesh.vertices[mesh.indices[i + 2]].bitangent = Vec3f(0f, 0f, 0f)
            mesh.vertices[mesh.indices[i]].tangent = mesh.vertices[mesh.indices[i]].tangent!!.add(tangent)
            mesh.vertices[mesh.indices[i]].bitangent = mesh.vertices[mesh.indices[i]].bitangent!!.add(bitangent)
            mesh.vertices[mesh.indices[i + 1]].tangent = mesh.vertices[mesh.indices[i + 1]].tangent!!.add(tangent)
            mesh.vertices[mesh.indices[i + 1]].bitangent = mesh.vertices[mesh.indices[i + 1]].bitangent!!.add(bitangent)
            mesh.vertices[mesh.indices[i + 2]].tangent = mesh.vertices[mesh.indices[i + 2]].tangent!!.add(tangent)
            mesh.vertices[mesh.indices[i + 2]].bitangent = mesh.vertices[mesh.indices[i + 2]].bitangent!!.add(bitangent)
            i += 3
        }
        for (vertex in mesh.vertices) {
            vertex.tangent = vertex.tangent!!.normalize()
            vertex.bitangent = vertex.bitangent!!.normalize()
        }
    }

    fun normalizePlane(plane: Vec4f): Vec4f {
        val mag: Float
        mag = Math.sqrt(plane.x * plane.x + plane.y * plane.y + (plane.z * plane.z).toDouble()).toFloat()
        plane.x = plane.x / mag
        plane.y = plane.y / mag
        plane.z = plane.z / mag
        plane.w = plane.w / mag
        return plane
    }

    fun texCoordsFromFontMap(x: Char): Array<Vec2f?> {
        val x_ = x.toInt() % 16 / 16.0f
        val y_ = x.toInt() / 16 / 16.0f
        val texCoords = arrayOfNulls<Vec2f>(4)
        texCoords[0] = Vec2f(x_, y_ + 1.0f / 16.0f)
        texCoords[1] = Vec2f(x_, y_)
        texCoords[2] = Vec2f(x_ + 1.0f / 16.0f, y_ + 1.0f / 16.0f)
        texCoords[3] = Vec2f(x_ + 1.0f / 16.0f, y_)
        return texCoords
    }

    fun initBitReversedIndices(n: Int): IntArray {
        val bitReversedIndices = IntArray(n)
        val bits = (ln(n.toDouble()) / ln(2.0)).toInt()
        for (i in 0 until n) {
            var x = Integer.reverse(i)
            x = Integer.rotateLeft(x, bits)
            bitReversedIndices[i] = x
        }
        return bitReversedIndices
    }

    fun getLog2N(n: Int): Int {
        return (ln(n.toDouble()) / ln(2.0)).toInt()
    }

    fun getMipLevelCount(metaData: ImageMetaData): Int {
        return getLog2N(if (metaData.height < metaData.width) metaData.width else metaData.height)
    }

    fun generateRandomKernel3D(kernelSize: Int): Array<Vec3f?> {
        val kernel = arrayOfNulls<Vec3f>(kernelSize)
        for (i in 0 until kernelSize) {
            kernel[i] = Vec3f(Math.random().toFloat() * 2 - 1,
                    Math.random().toFloat() * 2 - 1,
                    Math.random().toFloat())
            kernel[i]!!.normalize()
            var scale = i.toFloat() / kernelSize.toFloat()
            scale = Math.min(Math.max(0.01, scale * scale.toDouble()), 1.0).toFloat()
            kernel[i] = kernel[i]!!.mul(scale).mul(-1f)
        }
        return kernel
    }

    fun generateRandomKernel4D(kernelSize: Int): Array<Vec4f> {
        val kernel = ArrayList<Vec4f>(kernelSize)
        for (i in 0 until kernelSize) {
            kernel[i] = Vec4f(Math.random().toFloat() * 2 - 1,
                    Math.random().toFloat() * 2 - 1,
                    Math.random().toFloat(),
                    0f)
            kernel[i]!!.normalize()
            var scale = i.toFloat() / kernelSize.toFloat()
            scale = Math.min(Math.max(0.01, scale * scale.toDouble()), 1.0).toFloat()
            kernel[i] = kernel[i]!!.mul(scale).mul(-1f)
        }
        return kernel.toTypedArray()
    }
}