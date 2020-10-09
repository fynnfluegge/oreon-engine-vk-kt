package org.oreon.core.vk.image

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkImageViewCreateInfo
import org.oreon.core.vk.util.VkUtil

class VkImageView @JvmOverloads constructor(private val device: VkDevice, imageFormat: Int, image: Long,
                                            aspectMask: Int, mipLevels: Int = 1) {

    val handle: Long

    fun destroy() {
        VK10.vkDestroyImageView(device, handle, null)
    }

    init {
        val imageViewCreateInfo = VkImageViewCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .pNext(0)
                .viewType(VK10.VK_IMAGE_VIEW_TYPE_2D)
                .format(imageFormat)
                .image(image)
        imageViewCreateInfo
                .components()
                .r(VK10.VK_COMPONENT_SWIZZLE_R)
                .g(VK10.VK_COMPONENT_SWIZZLE_G)
                .b(VK10.VK_COMPONENT_SWIZZLE_B)
                .a(VK10.VK_COMPONENT_SWIZZLE_A)
        imageViewCreateInfo
                .subresourceRange()
                .aspectMask(aspectMask)
                .baseMipLevel(0)
                .levelCount(mipLevels)
                .baseArrayLayer(0)
                .layerCount(1)
        val pImageView = MemoryUtil.memAllocLong(1)
        val err = VK10.vkCreateImageView(device, imageViewCreateInfo, null, pImageView)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create image view: " + VkUtil.translateVulkanResult(err))
        }
        handle = pImageView[0]
        MemoryUtil.memFree(pImageView)
        imageViewCreateInfo.free()
    }
}