package org.oreon.core.vk.wrapper.image

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import org.oreon.core.image.ImageMetaData
import org.oreon.core.vk.image.VkImage

class Image2DDeviceLocal : VkImage {
    constructor(device: VkDevice?, memoryProperties: VkPhysicalDeviceMemoryProperties?,
                width: Int, height: Int, format: Int, usage: Int, samples: Int, mipLevels: Int) : super(device!!, width, height, 1, format, usage, samples, mipLevels) {
        allocate(memoryProperties, VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
        bindImageMemory()
    }

    constructor(device: VkDevice?, memoryProperties: VkPhysicalDeviceMemoryProperties?,
                width: Int, height: Int, format: Int, usage: Int, samples: Int, mipLevels: Int,
                metaData: ImageMetaData?) : super(device!!, width, height, 1, format, usage, samples, mipLevels) {
        this.metaData = metaData
        allocate(memoryProperties, VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
        bindImageMemory()
    }

    constructor(device: VkDevice?, memoryProperties: VkPhysicalDeviceMemoryProperties?,
                width: Int, height: Int, format: Int, usage: Int) : super(device!!, width, height, 1, format, usage, 1, 1) {
        allocate(memoryProperties, VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
        bindImageMemory()
    }

    constructor(device: VkDevice?, memoryProperties: VkPhysicalDeviceMemoryProperties?,
                width: Int, height: Int, format: Int, usage: Int, samples: Int) : super(device!!, width, height, 1, format, usage, samples, 1) {
        allocate(memoryProperties, VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
        bindImageMemory()
    }

    constructor(device: VkDevice?, memoryProperties: VkPhysicalDeviceMemoryProperties?,
                width: Int, height: Int, format: Int, usage: Int, metaData: ImageMetaData?) : super(device!!, width, height, 1, format, usage, 1, 1) {
        this.metaData = metaData
        allocate(memoryProperties, VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
        bindImageMemory()
    }
}