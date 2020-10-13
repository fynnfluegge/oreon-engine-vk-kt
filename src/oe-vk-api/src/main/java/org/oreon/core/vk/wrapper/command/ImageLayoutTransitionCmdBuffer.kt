package org.oreon.core.vk.wrapper.command

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkQueue
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.command.SubmitInfo
import org.oreon.core.vk.synchronization.Fence

class ImageLayoutTransitionCmdBuffer(device: VkDevice?, commandPool: Long) : CommandBuffer(device!!, commandPool, VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY) {
    fun record(image: Long, cmdBufferUsage: Int,
               oldLayout: Int, newLayout: Int, srcAccessMask: Int, dstAccessMask: Int,
               srcStageMask: Int, dstStageMask: Int, mipLevels: Int) {
        beginRecord(cmdBufferUsage)
        pipelineImageMemoryBarrierCmd(image, oldLayout, newLayout, srcAccessMask, dstAccessMask,
                srcStageMask, dstStageMask, 0, mipLevels)
        finishRecord()
    }

    fun submit(queue: VkQueue?) {
        val submitInfo = SubmitInfo(handlePointer)
        submitInfo.submit(queue)
    }

    fun submit(queue: VkQueue?, fence: Fence) {
        val submitInfo = SubmitInfo(handlePointer)
        submitInfo.fence = fence
        submitInfo.submit(queue)
        fence.waitForFence()
    }
}