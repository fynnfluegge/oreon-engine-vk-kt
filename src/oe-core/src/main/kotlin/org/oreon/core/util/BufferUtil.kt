package org.oreon.core.util

import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryUtil
import org.oreon.core.math.Matrix4f
import org.oreon.core.math.Vec2f
import org.oreon.core.math.Vec3f
import org.oreon.core.math.Vec4f
import org.oreon.core.model.Vertex
import org.oreon.core.model.Vertex.VertexLayout
import java.nio.ByteBuffer
import java.nio.DoubleBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

object BufferUtil {
    fun createFloatBuffer(size: Int): FloatBuffer {
        return BufferUtils.createFloatBuffer(size)
    }

    fun createIntBuffer(size: Int): IntBuffer {
        return BufferUtils.createIntBuffer(size)
    }

    fun createDoubleBuffer(size: Int): DoubleBuffer {
        return BufferUtils.createDoubleBuffer(size)
    }

    fun createFlippedBuffer(vararg values: Int): IntBuffer {
        val buffer = createIntBuffer(values.size)
        buffer.put(values)
        buffer.flip()
        return buffer
    }

    fun createFlippedBuffer(vararg values: Float): FloatBuffer {
        val buffer = createFloatBuffer(values.size)
        buffer.put(values)
        buffer.flip()
        return buffer
    }

    fun createFlippedBuffer(vararg values: Double): DoubleBuffer {
        val buffer = createDoubleBuffer(values.size)
        buffer.put(values)
        buffer.flip()
        return buffer
    }

    fun createFlippedBufferAOS(vertices: Array<Vertex>): FloatBuffer {
        val buffer = createFloatBuffer(vertices.size * Vertex.FLOATS)
        for (i in vertices.indices) {
            buffer.put(vertices[i].position.x)
            buffer.put(vertices[i].position.y)
            buffer.put(vertices[i].position.z)
            buffer.put(vertices[i].normal.x)
            buffer.put(vertices[i].normal.y)
            buffer.put(vertices[i].normal.z)
            buffer.put(vertices[i].uvCoord.x)
            buffer.put(vertices[i].uvCoord.y)
            if (vertices[i].tangent != null && vertices[i].bitangent != null) {
                buffer.put(vertices[i].tangent!!.x)
                buffer.put(vertices[i].tangent!!.y)
                buffer.put(vertices[i].tangent!!.z)
                buffer.put(vertices[i].bitangent!!.x)
                buffer.put(vertices[i].bitangent!!.y)
                buffer.put(vertices[i].bitangent!!.z)
            }
        }
        buffer.flip()
        return buffer
    }

    fun createFlippedBufferSOA(vertices: Array<Vertex>): FloatBuffer {
        val buffer = createFloatBuffer(vertices.size * Vertex.FLOATS)
        for (i in vertices.indices) {
            buffer.put(vertices[i].position.x)
            buffer.put(vertices[i].position.y)
            buffer.put(vertices[i].position.z)
        }
        for (i in vertices.indices) {
            buffer.put(vertices[i].normal.x)
            buffer.put(vertices[i].normal.y)
            buffer.put(vertices[i].normal.z)
        }
        for (i in vertices.indices) {
            buffer.put(vertices[i].uvCoord.x)
            buffer.put(vertices[i].uvCoord.y)
        }
        buffer.flip()
        return buffer
    }

    fun createFlippedBuffer(vector: Array<Vec3f>): FloatBuffer {
        val buffer = createFloatBuffer(vector.size * java.lang.Float.BYTES * 3)
        for (i in vector.indices) {
            buffer.put(vector[i].x)
            buffer.put(vector[i].y)
            buffer.put(vector[i].z)
        }
        buffer.flip()
        return buffer
    }

    fun createFlippedBuffer(vector: Array<Vec4f>): FloatBuffer {
        val buffer = createFloatBuffer(vector.size * java.lang.Float.BYTES * 4)
        for (i in vector.indices) {
            buffer.put(vector[i].x)
            buffer.put(vector[i].y)
            buffer.put(vector[i].z)
            buffer.put(vector[i].w)
        }
        buffer.flip()
        return buffer
    }

    fun createFlippedBuffer(vector: Vec3f): FloatBuffer {
        val buffer = createFloatBuffer(java.lang.Float.BYTES * 3)
        buffer.put(vector.x)
        buffer.put(vector.y)
        buffer.put(vector.z)
        buffer.flip()
        return buffer
    }

    fun createFlippedBuffer(vector: Vec2f): FloatBuffer {
        val buffer = createFloatBuffer(java.lang.Float.BYTES * 2)
        buffer.put(vector.x)
        buffer.put(vector.y)
        buffer.flip()
        return buffer
    }

    fun createFlippedBuffer(vector: Vec4f): FloatBuffer {
        val buffer = createFloatBuffer(java.lang.Float.BYTES * 4)
        buffer.put(vector.x)
        buffer.put(vector.y)
        buffer.put(vector.z)
        buffer.put(vector.w)
        buffer.flip()
        return buffer
    }

    fun createFlippedBuffer(vector: Array<Vec2f>): FloatBuffer {
        val buffer = createFloatBuffer(vector.size * java.lang.Float.BYTES * 2)
        for (i in vector.indices) {
            buffer.put(vector[i].x)
            buffer.put(vector[i].y)
        }
        buffer.flip()
        return buffer
    }

    fun createFlippedBuffer(vector: List<Vec2f>): FloatBuffer {
        val buffer = createFloatBuffer(vector.size * java.lang.Float.BYTES * 2)
        for (v in vector) {
            buffer.put(v.x)
            buffer.put(v.y)
        }
        buffer.flip()
        return buffer
    }

    fun createFlippedBuffer(matrix: Matrix4f): FloatBuffer {
        val buffer = createFloatBuffer(4 * 4)
        for (i in 0..3) for (j in 0..3) buffer.put(matrix[i, j])
        buffer.flip()
        return buffer
    }

    fun createFlippedBuffer(matrices: Array<Matrix4f>): FloatBuffer {
        val buffer = createFloatBuffer(4 * 4 * matrices.size)
        for (matrix in matrices) {
            for (i in 0..3) for (j in 0..3) buffer.put(matrix[i, j])
        }
        buffer.flip()
        return buffer
    }

    fun createByteBuffer(matrix: Matrix4f): ByteBuffer {
        val byteBuffer = MemoryUtil.memAlloc(java.lang.Float.BYTES * 16)
        val floatBuffer = byteBuffer.asFloatBuffer()
        floatBuffer.put(createFlippedBuffer(matrix))
        return byteBuffer
    }

    fun createByteBuffer(vector: Vec3f): ByteBuffer {
        val byteBuffer = MemoryUtil.memAlloc(java.lang.Float.BYTES * 3)
        val floatBuffer = byteBuffer.asFloatBuffer()
        floatBuffer.put(createFlippedBuffer(vector))
        return byteBuffer
    }

    fun createByteBuffer(vertices: Array<Vec2f>): ByteBuffer {
        val byteBuffer = MemoryUtil.memAlloc(java.lang.Float.BYTES * 2 * vertices.size)
        val floatBuffer = byteBuffer.asFloatBuffer()
        for (i in vertices.indices) {
            floatBuffer.put(vertices[i].x)
            floatBuffer.put(vertices[i].y)
        }
        return byteBuffer
    }

    fun createByteBuffer(vertices: Array<Vec3f>): ByteBuffer {
        val byteBuffer = MemoryUtil.memAlloc(java.lang.Float.BYTES * 3 * vertices.size)
        val floatBuffer = byteBuffer.asFloatBuffer()
        for (i in vertices.indices) {
            floatBuffer.put(vertices[i].x)
            floatBuffer.put(vertices[i].y)
            floatBuffer.put(vertices[i].z)
        }
        return byteBuffer
    }

    fun createByteBuffer(vertices: Array<Vec4f>): ByteBuffer {
        val byteBuffer = MemoryUtil.memAlloc(java.lang.Float.BYTES * 4 * vertices.size)
        val floatBuffer = byteBuffer.asFloatBuffer()
        for (i in vertices.indices) {
            floatBuffer.put(vertices[i].x)
            floatBuffer.put(vertices[i].y)
            floatBuffer.put(vertices[i].z)
            floatBuffer.put(vertices[i].w)
        }
        return byteBuffer
    }

    fun createByteBuffer(vertices: Array<Vertex>, layout: VertexLayout): ByteBuffer {
        val byteBuffer = allocateVertexByteBuffer(layout, vertices.size)
        val floatBuffer = byteBuffer.asFloatBuffer()
        for (i in vertices.indices) {
            if (layout === VertexLayout.POS2D ||
                    layout === VertexLayout.POS2D_UV) {
                floatBuffer.put(vertices[i].position.x)
                floatBuffer.put(vertices[i].position.y)
            } else {
                floatBuffer.put(vertices[i].position.x)
                floatBuffer.put(vertices[i].position.y)
                floatBuffer.put(vertices[i].position.z)
            }
            if (layout === VertexLayout.POS_NORMAL || layout === VertexLayout.POS_NORMAL_UV || layout === VertexLayout.POS_NORMAL_UV_TAN_BITAN) {
                floatBuffer.put(vertices[i].normal.x)
                floatBuffer.put(vertices[i].normal.y)
                floatBuffer.put(vertices[i].normal.z)
            }
            if (layout === VertexLayout.POS_NORMAL_UV || layout === VertexLayout.POS_UV || layout === VertexLayout.POS_NORMAL_UV_TAN_BITAN || layout === VertexLayout.POS2D_UV) {
                floatBuffer.put(vertices[i].uvCoord.x)
                floatBuffer.put(vertices[i].uvCoord.y)
            }
            if (layout === VertexLayout.POS_NORMAL_UV_TAN_BITAN) {
                floatBuffer.put(vertices[i].tangent!!.x)
                floatBuffer.put(vertices[i].tangent!!.y)
                floatBuffer.put(vertices[i].tangent!!.z)
                floatBuffer.put(vertices[i].bitangent!!.x)
                floatBuffer.put(vertices[i].bitangent!!.y)
                floatBuffer.put(vertices[i].bitangent!!.z)
            }
        }
        return byteBuffer
    }

    fun createByteBuffer(vararg values: Int): ByteBuffer {
        val byteBuffer = MemoryUtil.memAlloc(Integer.BYTES * values.size)
        val intBuffer = byteBuffer.asIntBuffer()
        intBuffer.put(values)
        intBuffer.flip()
        return byteBuffer
    }

    fun createByteBuffer(vararg values: Float): ByteBuffer {
        val byteBuffer = MemoryUtil.memAlloc(java.lang.Float.BYTES * values.size)
        val intBuffer = byteBuffer.asFloatBuffer()
        intBuffer.put(values)
        intBuffer.flip()
        return byteBuffer
    }

    fun createByteBuffer(floatBuffer: FloatBuffer): ByteBuffer {
        val byteBuffer = MemoryUtil.memAlloc(java.lang.Float.BYTES * floatBuffer.limit())
        val intBuffer = byteBuffer.asFloatBuffer()
        intBuffer.put(floatBuffer)
        intBuffer.flip()
        return byteBuffer
    }

    fun createByteBuffer(vector: Vec2f): ByteBuffer {
        return createByteBuffer(createFlippedBuffer(vector))
    }

    fun allocateVertexByteBuffer(layout: VertexLayout?, vertexCount: Int): ByteBuffer {
        val byteBuffer: ByteBuffer
        byteBuffer = when (layout) {
            VertexLayout.POS2D -> MemoryUtil.memAlloc(java.lang.Float.BYTES * 2 * vertexCount)
            VertexLayout.POS -> MemoryUtil.memAlloc(java.lang.Float.BYTES * 3 * vertexCount)
            VertexLayout.POS_UV -> MemoryUtil.memAlloc(java.lang.Float.BYTES * 5 * vertexCount)
            VertexLayout.POS2D_UV -> MemoryUtil.memAlloc(java.lang.Float.BYTES * 4 * vertexCount)
            VertexLayout.POS_NORMAL -> MemoryUtil.memAlloc(java.lang.Float.BYTES * 6 * vertexCount)
            VertexLayout.POS_NORMAL_UV -> MemoryUtil.memAlloc(java.lang.Float.BYTES * 8 * vertexCount)
            VertexLayout.POS_NORMAL_UV_TAN_BITAN -> MemoryUtil.memAlloc(java.lang.Float.BYTES * 14 * vertexCount)
            else -> MemoryUtil.memAlloc(0)
        }
        return byteBuffer
    }

    fun resizeBuffer(buffer: ByteBuffer, newCapacity: Int): ByteBuffer {
        val newBuffer = BufferUtils.createByteBuffer(newCapacity)
        buffer.flip()
        newBuffer.put(buffer)
        return newBuffer
    }
}