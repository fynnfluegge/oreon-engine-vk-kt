package org.oreon.core.vk.image

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkSamplerCreateInfo
import org.oreon.core.vk.util.VkUtil

class VkSampler(private val device: VkDevice, filterMode: Int,
                anisotropic: Boolean, maxAnisotropy: Float, mipmapMode: Int, maxLod: Float,
                addressMode: Int) {

    val handle: Long

    fun destroy() {
        VK10.vkDestroySampler(device, handle, null)
    }

    init {
        val createInfo = VkSamplerCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                .magFilter(filterMode)
                .minFilter(filterMode)
                .addressModeU(addressMode)
                .addressModeV(addressMode)
                .addressModeW(addressMode)
                .anisotropyEnable(anisotropic)
                .maxAnisotropy(maxAnisotropy)
                .borderColor(VK10.VK_BORDER_COLOR_INT_OPAQUE_BLACK)
                .unnormalizedCoordinates(false)
                .compareEnable(false)
                .compareOp(VK10.VK_COMPARE_OP_ALWAYS)
                .mipmapMode(mipmapMode)
                .mipLodBias(0f)
                .minLod(0f)
                .maxLod(maxLod)
        val pBuffer = MemoryUtil.memAllocLong(1)
        val err = VK10.vkCreateSampler(device, createInfo, null, pBuffer)
        handle = pBuffer[0]
        MemoryUtil.memFree(pBuffer)
        createInfo.free()
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create sampler: " + VkUtil.translateVulkanResult(err))
        }
    }
}