package org.oreon.core.vk.descriptor

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo
import org.lwjgl.vulkan.VkDevice
import org.oreon.core.vk.util.VkUtil
import java.nio.LongBuffer

class DescriptorSetLayout(private val device: VkDevice, bindingCount: Int) {

    var handle: Long = 0
    lateinit var handlePointer: LongBuffer
    private val layoutBindings: VkDescriptorSetLayoutBinding.Buffer = VkDescriptorSetLayoutBinding.calloc(bindingCount)

    fun create() {
        layoutBindings.flip()
        val layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pBindings(layoutBindings)
                .flags(0)
        handlePointer = MemoryUtil.memAllocLong(1)
        val err = VK10.vkCreateDescriptorSetLayout(device, layoutInfo, null, handlePointer)
        handle = handlePointer.get(0)
        layoutBindings.clear()
        layoutBindings.free()
        layoutInfo.free()
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create DescriptorSetLayout: " + VkUtil.translateVulkanResult(err))
        }
    }

    fun addLayoutBinding(binding: Int, type: Int, stageflags: Int) {
        val layoutBinding = VkDescriptorSetLayoutBinding.calloc()
                .binding(binding)
                .descriptorType(type)
                .descriptorCount(1)
                .stageFlags(stageflags)
                .pImmutableSamplers(null)
        layoutBindings.put(layoutBinding)
    }

    fun destroy() {
        VK10.vkDestroyDescriptorSetLayout(device, handle, null)
        handle = -1
    }

}