package org.oreon.core.vk.scenegraph

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VkQueue
import org.oreon.core.model.Mesh
import org.oreon.core.model.Vertex.VertexLayout
import org.oreon.core.scenegraph.NodeComponent
import org.oreon.core.util.BufferUtil
import org.oreon.core.vk.command.CommandPool
import org.oreon.core.vk.memory.VkBuffer
import org.oreon.core.vk.wrapper.buffer.VkBufferHelper
import java.nio.ByteBuffer

class VkMeshData(val device: VkDevice? = null, val memoryProperties: VkPhysicalDeviceMemoryProperties? = null,
                 val commandPool: CommandPool? = null, val queue: VkQueue? = null, val mesh: Mesh? = null, val vertexLayout: VertexLayout? = null,
                 val vertexBuffer: ByteBuffer? = null, var vertexBufferObject: VkBuffer? = null,
                 val indexBuffer: ByteBuffer? = null, var indexBufferObject: VkBuffer? = null,
                 var vertexCount: Int? = null) : NodeComponent() {

    var indexCount: Int = 0

    fun create() {

        val vertexByteBuffer = BufferUtil.createByteBuffer(mesh?.vertices, vertexLayout)
        val indexByteBuffer = BufferUtil.createByteBuffer(*mesh!!.indices)
        vertexCount = mesh.vertices.size
        indexCount = mesh.indices.size
        vertexBufferObject = VkBufferHelper.createDeviceLocalBuffer(
                device, memoryProperties,
                commandPool!!.handle, queue,
                vertexByteBuffer, VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
        indexBufferObject = VkBufferHelper.createDeviceLocalBuffer(
                device, memoryProperties,
                commandPool.handle, queue,
                indexByteBuffer, VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT)
    }

    override fun shutdown() {
        vertexBufferObject?.destroy()
        indexBufferObject?.destroy()
    }
}