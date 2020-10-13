package org.oreon.core.vk.wrapper.buffer

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import org.oreon.core.vk.memory.VkBuffer
import java.nio.ByteBuffer

class StagingBuffer(device: VkDevice?,
                    memoryProperties: VkPhysicalDeviceMemoryProperties?,
                    dataBuffer: ByteBuffer) : VkBuffer(device!!, dataBuffer.limit(), VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT) {
    init {
        allocate(memoryProperties,
                VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
        bindBufferMemory()
        mapMemory(dataBuffer)
    }
}