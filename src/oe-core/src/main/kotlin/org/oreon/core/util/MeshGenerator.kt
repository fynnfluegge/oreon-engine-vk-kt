package org.oreon.core.util

import org.oreon.core.math.Vec2f
import org.oreon.core.math.Vec3f
import org.oreon.core.model.Mesh
import org.oreon.core.model.Vertex
import org.oreon.core.model.Vertex.VertexLayout

object MeshGenerator {

    fun NDCQuad2D() : Mesh {

        val vertices = ArrayList<Vertex>(4)
        val indices = intArrayOf(0, 2, 1, 1, 2, 3)
        vertices.add(Vertex(Vec3f(-1f, -1f, 0f), Vec2f(0f, 0f)))
        vertices.add(Vertex(Vec3f(1f, -1f, 0f), Vec2f(1f, 0f)))
        vertices.add(Vertex(Vec3f(-1f, 1f, 0f), Vec2f(0f, 1f)))
        vertices.add(Vertex(Vec3f(1f, 1f, 0f), Vec2f(1f, 1f)))
        val quad = Mesh(vertices.toTypedArray(), indices)
        quad.vertexLayout = VertexLayout.POS_UV
        return quad
    }

    fun NDCQuad2Drot180(): Mesh {
        val vertices = ArrayList<Vertex>(4)
        val indices = intArrayOf(0, 2, 1, 1, 2, 3)
        vertices[0] = Vertex(Vec3f(-1f, -1f, 0f), Vec2f(0f, 1f))
        vertices[1] = Vertex(Vec3f(1f, -1f, 0f), Vec2f(1f, 1f))
        vertices[2] = Vertex(Vec3f(-1f, 1f, 0f), Vec2f(0f, 0f))
        vertices[3] = Vertex(Vec3f(1f, 1f, 0f), Vec2f(1f, 0f))
        val quad = Mesh(vertices.toTypedArray(), indices)
        quad.vertexLayout = VertexLayout.POS_UV
        return quad
    }

    fun Cube(): Mesh {
        val vertices = ArrayList<Vertex>(24)
        val indices = intArrayOf(0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7, 8, 9, 10, 8, 10, 11, 12, 13,
                14, 12, 14, 15, 16, 17, 18, 16, 18, 19, 20, 21, 22, 20, 22, 23)
        vertices[0] = Vertex(Vec3f(-1f, -1f, -1f), Vec2f(0f, 1f))
        vertices[1] = Vertex(Vec3f(-1f, 1f, -1f), Vec2f(0f, 0f))
        vertices[2] = Vertex(Vec3f(1f, 1f, -1f), Vec2f(1f, 0f))
        vertices[3] = Vertex(Vec3f(1f, -1f, -1f), Vec2f(1f, 1f))
        vertices[4] = Vertex(Vec3f(-1f, -1f, 1f), Vec2f(0f, 1f))
        vertices[5] = Vertex(Vec3f(-1f, 1f, 1f), Vec2f(0f, 0f))
        vertices[6] = Vertex(Vec3f(-1f, 1f, -1f), Vec2f(1f, 0f))
        vertices[7] = Vertex(Vec3f(-1f, -1f, -1f), Vec2f(1f, 1f))
        vertices[8] = Vertex(Vec3f(1f, -1f, 1f), Vec2f(0f, 1f))
        vertices[9] = Vertex(Vec3f(1f, 1f, 1f), Vec2f(0f, 0f))
        vertices[10] = Vertex(Vec3f(-1f, 1f, 1f), Vec2f(1f, 0f))
        vertices[11] = Vertex(Vec3f(-1f, -1f, 1f), Vec2f(1f, 1f))
        vertices[12] = Vertex(Vec3f(1f, -1f, -1f), Vec2f(0f, 1f))
        vertices[13] = Vertex(Vec3f(1f, 1f, -1f), Vec2f(0f, 0f))
        vertices[14] = Vertex(Vec3f(1f, 1f, 1f), Vec2f(1f, 0f))
        vertices[15] = Vertex(Vec3f(1f, -1f, 1f), Vec2f(1f, 1f))
        vertices[16] = Vertex(Vec3f(-1f, 1f, -1f), Vec2f(0f, 1f))
        vertices[17] = Vertex(Vec3f(-1f, 1f, 1f), Vec2f(0f, 0f))
        vertices[18] = Vertex(Vec3f(1f, 1f, 1f), Vec2f(1f, 0f))
        vertices[19] = Vertex(Vec3f(1f, 1f, -1f), Vec2f(1f, 1f))
        vertices[20] = Vertex(Vec3f(1f, -1f, -1f), Vec2f(0f, 1f))
        vertices[21] = Vertex(Vec3f(1f, -1f, 1f), Vec2f(0f, 0f))
        vertices[22] = Vertex(Vec3f(-1f, -1f, 1f), Vec2f(1f, 0f))
        vertices[23] = Vertex(Vec3f(-1f, -1f, -1f), Vec2f(1f, 1f))
        return Mesh(vertices.toTypedArray(), indices)
    }

    fun TerrainChunkMesh(): Array<Vec2f> {

        // 16 vertices for each patch
        val vertices = ArrayList<Vec2f>()
        var index = 0
        vertices[index++] = Vec2f(0f, 0f)
        vertices[index++] = Vec2f(0.333f, 0f)
        vertices[index++] = Vec2f(0.666f, 0f)
        vertices[index++] = Vec2f(1f, 0f)
        vertices[index++] = Vec2f(0f, 0.333f)
        vertices[index++] = Vec2f(0.333f, 0.333f)
        vertices[index++] = Vec2f(0.666f, 0.333f)
        vertices[index++] = Vec2f(1f, 0.333f)
        vertices[index++] = Vec2f(0f, 0.666f)
        vertices[index++] = Vec2f(0.333f, 0.666f)
        vertices[index++] = Vec2f(0.666f, 0.666f)
        vertices[index++] = Vec2f(1f, 0.666f)
        vertices[index++] = Vec2f(0f, 1f)
        vertices[index++] = Vec2f(0.333f, 1f)
        vertices[index++] = Vec2f(0.666f, 1f)
        vertices[index++] = Vec2f(1f, 1f)
        return vertices.toTypedArray()
    }

    fun generatePatch2D4x4(patches: Int): Array<Vec2f> {

        // 16 vertices for each patch
        val vertices = ArrayList<Vec2f>(patches * patches * 16)

        val dx = 1f / patches
        val dy = 1f / patches

        for (i in 0 until patches){
            for (j in 0 until patches){
                vertices.add(Vec2f(i.toFloat() * dx, j.toFloat() * dy))
                vertices.add(Vec2f(i.toFloat() * dx + dx * 0.33f, j.toFloat() * dy))
                vertices.add(Vec2f(i.toFloat() * dx + dx * 0.66f, j.toFloat() * dy))
                vertices.add(Vec2f(i.toFloat() * dx + dx, j.toFloat() * dy))

                vertices.add(Vec2f(i.toFloat() * dx, j.toFloat() * dy + dy * 0.33f))
                vertices.add(Vec2f(i.toFloat() * dx + dx * 0.33f, j.toFloat() * dy + dy * 0.33f))
                vertices.add(Vec2f(i.toFloat() * dx + dx * 0.66f, j.toFloat() * dy + dy * 0.33f))
                vertices.add(Vec2f(i.toFloat() * dx + dx, j.toFloat() * dy + dy * 0.33f))

                vertices.add(Vec2f(i.toFloat() * dx, j.toFloat() * dy + dy * 0.66f))
                vertices.add(Vec2f(i.toFloat() * dx + dx * 0.33f, j.toFloat() * dy + dy * 0.66f))
                vertices.add(Vec2f(i.toFloat() * dx + dx * 0.66f, j.toFloat() * dy + dy * 0.66f))
                vertices.add(Vec2f(i.toFloat() * dx + dx, j.toFloat() * dy + dy * 0.66f))

                vertices.add(Vec2f(i.toFloat() * dx, j.toFloat() * dy + dy))
                vertices.add(Vec2f(i.toFloat() * dx + dx * 0.33f, j.toFloat() * dy + dy))
                vertices.add(Vec2f(i.toFloat() * dx + dx * 0.66f, j.toFloat() * dy + dy))
                vertices.add(Vec2f(i.toFloat() * dx + dx, j.toFloat() * dy + dy))
            }
        }

        return vertices.toTypedArray()
    }
}