package org.oreon.core.vk.synchronization

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkFenceCreateInfo
import org.oreon.core.vk.util.VkUtil
import java.nio.LongBuffer

class Fence(private val device: VkDevice) {

    val handle: Long
    private val pHandle: LongBuffer

    fun reset() {
        VkUtil.vkCheckResult(VK10.vkResetFences(device, handle))
    }

    fun waitForFence() {
        VkUtil.vkCheckResult(VK10.vkWaitForFences(device, pHandle, true, 1000000000L))
    }

    fun destroy() {
        VK10.vkDestroyFence(device, handle, null)
    }

    init {
        val createInfo = VkFenceCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                .pNext(0)
                .flags(VK10.VK_FENCE_CREATE_SIGNALED_BIT)
        pHandle = MemoryUtil.memAllocLong(1)
        VkUtil.vkCheckResult(VK10.vkCreateFence(device, createInfo, null, pHandle))
        handle = pHandle[0]
        createInfo.free()
    }
}