package org.oreon.core.vk.memory

import lombok.Getter
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.oreon.core.vk.device.DeviceCapabilities.getMemoryTypeIndex
import org.oreon.core.vk.util.VkUtil
import java.nio.ByteBuffer

open class VkBuffer(private val device: VkDevice, size: Int, usage: Int) {
    @Getter
    private val handle: Long

    @Getter
    private var memory: Long = 0
    private var allocationSize: Long = 0
    fun allocate(memoryProperties: VkPhysicalDeviceMemoryProperties?,
                 memoryPropertyFlags: Int) {
        val memRequirements = VkMemoryRequirements.calloc()
        VK10.vkGetBufferMemoryRequirements(device, handle, memRequirements)
        val memoryTypeIndex = MemoryUtil.memAllocInt(1)
        if (!getMemoryTypeIndex(memoryProperties!!,
                        memRequirements.memoryTypeBits(),
                        memoryPropertyFlags,
                        memoryTypeIndex)) {
            throw AssertionError("No memory Type found")
        }
        allocationSize = memRequirements.size()
        val memAlloc = VkMemoryAllocateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .pNext(0)
                .allocationSize(allocationSize)
                .memoryTypeIndex(memoryTypeIndex[0])
        val pMemory = MemoryUtil.memAllocLong(1)
        val err = VK10.vkAllocateMemory(device, memAlloc, null, pMemory)
        memory = pMemory[0]
        MemoryUtil.memFree(pMemory)
        memAlloc.free()
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to allocate buffer memory: " + VkUtil.translateVulkanResult(err))
        }
    }

    fun bindBufferMemory() {
        val err = VK10.vkBindBufferMemory(device, handle, memory, 0)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to bind memory to buffer: " + VkUtil.translateVulkanResult(err))
        }
    }

    fun mapMemory(buffer: ByteBuffer) {
        val pData = MemoryUtil.memAllocPointer(1)
        val err = VK10.vkMapMemory(device, memory, 0, buffer.remaining().toLong(), 0, pData)
        val data = pData[0]
        MemoryUtil.memFree(pData)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to map buffer memory: " + VkUtil.translateVulkanResult(err))
        }
        MemoryUtil.memCopy(MemoryUtil.memAddress(buffer), data, buffer.remaining().toLong())
        MemoryUtil.memFree(buffer)
        VK10.vkUnmapMemory(device, memory)
    }

    fun destroy() {
        VK10.vkFreeMemory(device, memory, null)
        VK10.vkDestroyBuffer(device, handle, null)
    }

    init {
        val bufInfo = VkBufferCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .pNext(0)
                .size(size.toLong())
                .usage(usage)
                .sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE)
                .flags(0)
        val pBuffer = MemoryUtil.memAllocLong(1)
        val err = VK10.vkCreateBuffer(device, bufInfo, null, pBuffer)
        handle = pBuffer[0]
        MemoryUtil.memFree(pBuffer)
        bufInfo.free()
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create buffer: " + VkUtil.translateVulkanResult(err))
        }
    }
}