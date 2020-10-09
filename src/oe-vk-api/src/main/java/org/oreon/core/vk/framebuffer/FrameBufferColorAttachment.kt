package org.oreon.core.vk.framebuffer

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import org.oreon.core.vk.image.VkImageView
import org.oreon.core.vk.wrapper.image.Image2DDeviceLocal
import org.oreon.core.vk.wrapper.image.VkImageBundle

class FrameBufferColorAttachment(device: VkDevice, memoryProperties: VkPhysicalDeviceMemoryProperties,
                                 width: Int, height: Int, format: Int, samples: Int) : VkImageBundle() {
    init {
        image = Image2DDeviceLocal(device, memoryProperties, width, height, format,
                VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK10.VK_IMAGE_USAGE_SAMPLED_BIT
                        or VK10.VK_IMAGE_USAGE_STORAGE_BIT,
                samples)
        imageView = VkImageView(device, image.format, image.handle,
                VK10.VK_IMAGE_ASPECT_COLOR_BIT)


    }
}