package org.oreon.core.vk.image

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.oreon.core.image.Image
import org.oreon.core.vk.device.DeviceCapabilities.getMemoryTypeIndex
import org.oreon.core.vk.util.VkUtil
import java.nio.ByteBuffer

open class VkImage(private val device: VkDevice, width: Int, height: Int, depth: Int,
                   var format: Int, usage: Int, samples: Int, mipLevels: Int) : Image() {
    var handle: Long
    var memory: Long = 0
    private var allocationSize: Long = 0
    fun allocate(memoryProperties: VkPhysicalDeviceMemoryProperties?,
                 memoryPropertyFlags: Int) {
        val memRequirements = VkMemoryRequirements.calloc()
        VK10.vkGetImageMemoryRequirements(device, handle, memRequirements)
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
            throw AssertionError("Failed to allocate image memory: " + VkUtil.translateVulkanResult(err))
        }
    }

    fun bindImageMemory() {
        val err = VK10.vkBindImageMemory(device, handle, memory, 0)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to bind memory to image buffer: " + VkUtil.translateVulkanResult(err))
        }
    }

    fun mapMemory(imageBuffer: ByteBuffer) {
        val pData = MemoryUtil.memAllocPointer(1)
        val err = VK10.vkMapMemory(device, memory, 0, imageBuffer.remaining().toLong(), 0, pData)
        val data = pData[0]
        MemoryUtil.memFree(pData)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to map image memory: " + VkUtil.translateVulkanResult(err))
        }
        MemoryUtil.memCopy(MemoryUtil.memAddress(imageBuffer), data, imageBuffer.remaining().toLong())
        MemoryUtil.memFree(imageBuffer)
        VK10.vkUnmapMemory(device, memory)
    }

    fun destroy() {
        VK10.vkFreeMemory(device, memory, null)
        VK10.vkDestroyImage(device, handle, null)
    }

    init {
        val extent = VkExtent3D.calloc()
                .width(width)
                .height(height)
                .depth(depth)
        val createInfo = VkImageCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                .imageType(VK10.VK_IMAGE_TYPE_2D)
                .extent(extent)
                .mipLevels(mipLevels)
                .arrayLayers(1)
                .format(format)
                .tiling(VK10.VK_IMAGE_TILING_OPTIMAL)
                .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
                .usage(usage)
                .sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE)
                .samples(VkUtil.getSampleCountBit(samples))
                .flags(0)
        val pBuffer = MemoryUtil.memAllocLong(1)
        val err = VK10.vkCreateImage(device, createInfo, null, pBuffer)
        handle = pBuffer[0]
        MemoryUtil.memFree(pBuffer)
        createInfo.free()
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create image: " + VkUtil.translateVulkanResult(err))
        }
    }
}