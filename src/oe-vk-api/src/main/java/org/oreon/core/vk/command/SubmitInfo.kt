package org.oreon.core.vk.command

import org.lwjgl.PointerBuffer
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkQueue
import org.lwjgl.vulkan.VkSubmitInfo
import org.oreon.core.vk.synchronization.Fence
import org.oreon.core.vk.util.VkUtil
import java.nio.IntBuffer
import java.nio.LongBuffer

class SubmitInfo() {

    val handle: VkSubmitInfo = VkSubmitInfo.calloc()
            .sType(VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO)
            .pNext(0)
    var fence: Fence? = null

    constructor(buffers: PointerBuffer?) : this() {
        setCommandBuffers(buffers)
    }

    fun setCommandBuffers(buffers: PointerBuffer?) {
        handle.pCommandBuffers(buffers)
    }

    fun setWaitSemaphores(semaphores: LongBuffer) {
        handle.waitSemaphoreCount(semaphores.remaining())
        handle.pWaitSemaphores(semaphores)
    }

    fun setSignalSemaphores(semaphores: LongBuffer?) {
        handle.pSignalSemaphores(semaphores)
    }

    fun setWaitDstStageMask(waitDstStageMasks: IntBuffer?) {
        handle.pWaitDstStageMask(waitDstStageMasks)
    }

    fun clearWaitSemaphores() {
        handle.waitSemaphoreCount(0)
        handle.pWaitSemaphores(null)
    }

    fun clearSignalSemaphores() {
        handle.pSignalSemaphores(null)
    }

    fun submit(queue: VkQueue?) {
        fence?.reset()
        VkUtil.vkCheckResult(VK10.vkQueueSubmit(queue, handle,
                fence?.handle ?: VK10.VK_NULL_HANDLE))
    }

}