package org.oreon.core.vk.wrapper.buffer

import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VkQueue
import org.oreon.core.vk.memory.VkBuffer
import org.oreon.core.vk.synchronization.Fence
import org.oreon.core.vk.wrapper.command.BufferCopyCmdBuffer
import java.nio.ByteBuffer

object VkBufferHelper {
    fun createDeviceLocalBuffer(device: VkDevice?,
                                memoryProperties: VkPhysicalDeviceMemoryProperties?,
                                commandPool: Long, queue: VkQueue?, dataBuffer: ByteBuffer, usage: Int): VkBuffer {
        val stagingBuffer = StagingBuffer(device, memoryProperties, dataBuffer)
        val deviceLocalBuffer = DeviceLocalBuffer(device, memoryProperties,
                dataBuffer.limit(), usage)
        val bufferCopyCommand = BufferCopyCmdBuffer(device, commandPool)
        bufferCopyCommand.record(stagingBuffer.handle,
                deviceLocalBuffer.handle, 0, 0, dataBuffer.limit().toLong())
        val fence = Fence(device!!)
        bufferCopyCommand.submit(queue, fence)
        bufferCopyCommand.destroy()
        stagingBuffer.destroy()
        return deviceLocalBuffer
    }
}