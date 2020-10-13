package org.oreon.core.vk.wrapper.buffer

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import org.oreon.core.vk.memory.VkBuffer
import java.nio.ByteBuffer

class VkUniformBuffer(device: VkDevice?, memoryProperties: VkPhysicalDeviceMemoryProperties?,
                      data: ByteBuffer) : VkBuffer(device!!, data.limit(), VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT) {
    fun updateData(data: ByteBuffer?) {
        mapMemory(data!!)
    }

    init {
        allocate(memoryProperties,
                VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
        bindBufferMemory()
        mapMemory(data)
    }
}