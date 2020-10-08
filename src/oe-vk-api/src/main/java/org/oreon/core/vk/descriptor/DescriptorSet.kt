package org.oreon.core.vk.descriptor

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.oreon.core.vk.util.VkUtil
import java.nio.LongBuffer

open class DescriptorSet(private val device: VkDevice, private val descriptorPool: Long, layouts: LongBuffer?) {

    var handle: Long
    fun updateDescriptorBuffer(buffer: Long, range: Long, offset: Long,
                               binding: Int, descriptorType: Int) {
        val bufferInfo = VkDescriptorBufferInfo.calloc(1)
                .buffer(buffer)
                .offset(offset)
                .range(range)
        val writeDescriptor = VkWriteDescriptorSet.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .dstSet(handle)
                .dstBinding(binding)
                .dstArrayElement(0)
                .descriptorType(descriptorType)
                .pBufferInfo(bufferInfo)
        VK10.vkUpdateDescriptorSets(device, writeDescriptor, null)
    }

    fun updateDescriptorImageBuffer(imageView: Long, imageLayout: Int,
                                    sampler: Long, binding: Int, descriptorType: Int) {
        val imageInfo = VkDescriptorImageInfo.calloc(1)
                .imageLayout(imageLayout)
                .imageView(imageView)
                .sampler(sampler)
        val writeDescriptor = VkWriteDescriptorSet.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .dstSet(handle)
                .dstBinding(binding)
                .dstArrayElement(0)
                .descriptorType(descriptorType)
                .pImageInfo(imageInfo)
        VK10.vkUpdateDescriptorSets(device, writeDescriptor, null)
    }

    fun destroy() {
        VK10.vkFreeDescriptorSets(device, descriptorPool, handle)
        handle = -1
    }

    init {
        val allocateInfo = VkDescriptorSetAllocateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                .descriptorPool(descriptorPool)
                .pSetLayouts(layouts)
        val pDescriptorSet = MemoryUtil.memAllocLong(1)
        val err = VK10.vkAllocateDescriptorSets(device, allocateInfo, pDescriptorSet)
        handle = pDescriptorSet[0]
        allocateInfo.free()
        MemoryUtil.memFree(pDescriptorSet)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create Descriptor Set: " + VkUtil.translateVulkanResult(err))
        }
    }
}