package org.oreon.core.vk.command

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkCommandPoolCreateInfo
import org.lwjgl.vulkan.VkDevice
import org.oreon.core.vk.util.VkUtil

class CommandPool(private val device: VkDevice, queueFamilyIndex: Int) {

    val handle: Long

    fun destroy() {
        VK10.vkDestroyCommandPool(device, handle, null)
    }

    init {
        val cmdPoolInfo = VkCommandPoolCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .queueFamilyIndex(queueFamilyIndex)
                .flags(VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
        val pCmdPool = MemoryUtil.memAllocLong(1)
        val err = VK10.vkCreateCommandPool(device, cmdPoolInfo, null, pCmdPool)
        handle = pCmdPool[0]
        cmdPoolInfo.free()
        MemoryUtil.memFree(pCmdPool)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create command pool: " + VkUtil.translateVulkanResult(err))
        }
    }
}