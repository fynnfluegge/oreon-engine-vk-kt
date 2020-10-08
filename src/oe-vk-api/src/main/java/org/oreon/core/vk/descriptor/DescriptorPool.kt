package org.oreon.core.vk.descriptor

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo
import org.lwjgl.vulkan.VkDescriptorPoolSize
import org.lwjgl.vulkan.VkDevice
import org.oreon.core.vk.util.VkUtil

class DescriptorPool(private val device: VkDevice, poolSizeCount: Int) {

    var handle: Long = 0
    private val poolSizes: VkDescriptorPoolSize.Buffer = VkDescriptorPoolSize.calloc(poolSizeCount)
    private var maxSets: Int = 0
    fun create() {
        poolSizes.flip()
        val createInfo = VkDescriptorPoolCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                .pPoolSizes(poolSizes)
                .maxSets(maxSets)
                .flags(VK10.VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
        val pDescriptorPool = MemoryUtil.memAllocLong(1)
        val err = VK10.vkCreateDescriptorPool(device, createInfo, null, pDescriptorPool)
        handle = pDescriptorPool[0]
        poolSizes.free()
        createInfo.free()
        MemoryUtil.memFree(pDescriptorPool)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create Descriptor pool: " + VkUtil.translateVulkanResult(err))
        }
    }

    fun addPoolSize(type: Int, descriptorCount: Int) {
        val poolSize = VkDescriptorPoolSize.calloc()
                .type(type)
                .descriptorCount(descriptorCount)
        poolSizes.put(poolSize)
        maxSets += descriptorCount
    }

    fun destroy() {
        VK10.vkDestroyDescriptorPool(device, handle, null)
    }

}