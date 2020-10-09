package org.oreon.core.vk.framebuffer

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkFramebufferCreateInfo
import org.oreon.core.vk.util.VkUtil
import java.nio.LongBuffer

class VkFrameBuffer(private val device: VkDevice, width: Int, height: Int, layers: Int,
                    pAttachments: LongBuffer?, renderPass: Long) {
    val handle: Long

    fun destroy() {
        VK10.vkDestroyFramebuffer(device, handle, null)
    }

    init {
        val framebufferInfo = VkFramebufferCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                .pAttachments(pAttachments)
                .flags(0)
                .height(height)
                .width(width)
                .layers(layers)
                .pNext(0)
                .renderPass(renderPass)
        val pFramebuffer = MemoryUtil.memAllocLong(1)
        val err = VK10.vkCreateFramebuffer(device, framebufferInfo, null, pFramebuffer)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create framebuffer: " + VkUtil.translateVulkanResult(err))
        }
        handle = pFramebuffer[0]
        framebufferInfo.free()
        MemoryUtil.memFree(pFramebuffer)
        MemoryUtil.memFree(pAttachments)
    }
}