package org.oreon.core.vk.wrapper.buffer

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import org.oreon.core.vk.memory.VkBuffer

class DeviceLocalBuffer(device: VkDevice?, memoryProperties: VkPhysicalDeviceMemoryProperties?,
                        size: Int, usage: Int) : VkBuffer(device!!, size, VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT or usage) {
    init {
        allocate(memoryProperties, VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
        bindBufferMemory()
    }
}