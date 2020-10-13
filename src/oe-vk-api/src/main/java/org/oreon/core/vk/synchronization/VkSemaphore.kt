package org.oreon.core.vk.synchronization

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkSemaphoreCreateInfo
import org.oreon.core.vk.util.VkUtil
import java.nio.LongBuffer

class VkSemaphore(private val device: VkDevice) {
    var handle: Long
    var handlePointer: LongBuffer
    fun destroy() {
        VK10.vkDestroySemaphore(device, handle, null)
    }

    init {
        val semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                .pNext(0)
                .flags(0)
        handlePointer = MemoryUtil.memAllocLong(1)
        val err = VK10.vkCreateSemaphore(device, semaphoreCreateInfo, null, handlePointer)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create semaphore: " + VkUtil.translateVulkanResult(err))
        }
        handle = handlePointer[0]
        semaphoreCreateInfo.free()
    }
}