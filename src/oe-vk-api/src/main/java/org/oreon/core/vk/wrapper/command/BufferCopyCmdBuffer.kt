package org.oreon.core.vk.wrapper.command

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkQueue
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.command.SubmitInfo
import org.oreon.core.vk.synchronization.Fence

class BufferCopyCmdBuffer(device: VkDevice?, commandPool: Long) : CommandBuffer(device!!, commandPool, VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY) {
    fun record(srcBuffer: Long, dstBuffer: Long, srcOffset: Long, dstOffset: Long, size: Long) {
        beginRecord(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
        copyBufferCmd(srcBuffer, dstBuffer, srcOffset, dstOffset, size)
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