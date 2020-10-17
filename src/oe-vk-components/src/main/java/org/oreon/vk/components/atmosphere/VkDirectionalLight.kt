package org.oreon.vk.components.atmosphere

import lombok.Getter
import org.lwjgl.vulkan.VK10
import org.oreon.core.light.DirectionalLight
import org.oreon.core.util.BufferUtil.createByteBuffer
import org.oreon.core.vk.context.DeviceManager.DeviceType
import org.oreon.core.vk.context.VkContext.deviceManager
import org.oreon.core.vk.context.VkContext.resources
import org.oreon.core.vk.context.VkResources.VkDescriptorName
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import org.oreon.core.vk.wrapper.buffer.VkUniformBuffer
import org.oreon.core.vk.wrapper.descriptor.VkDescriptor

@Getter
class VkDirectionalLight : DirectionalLight() {
    private val ubo_light: VkUniformBuffer
    private val descriptorSet: DescriptorSet
    private val descriptorSetLayout: DescriptorSetLayout
    override fun updateLightUbo() {
        ubo_light.mapMemory(createByteBuffer(floatBufferLight))
    }

    override fun updateMatricesUbo() {}

    init {
        val device = deviceManager.getLogicalDevice(DeviceType.MAJOR_GRAPHICS_DEVICE)
        val memoryProperties = deviceManager.getPhysicalDevice(DeviceType.MAJOR_GRAPHICS_DEVICE).memoryProperties
        ubo_light = VkUniformBuffer(device.handle, memoryProperties, createByteBuffer(floatBufferLight))
        descriptorSetLayout = DescriptorSetLayout(device.handle, 1)
        descriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT or VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorSetLayout.create()
        descriptorSet = DescriptorSet(device.handle,
                deviceManager.getLogicalDevice(DeviceType.MAJOR_GRAPHICS_DEVICE)
                        .getDescriptorPool(Thread.currentThread().id)!!.handle,
                descriptorSetLayout.handlePointer)
        descriptorSet.updateDescriptorBuffer(ubo_light.handle, lightBufferSize.toLong(), 0, 0,
                VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
        resources.descriptors[VkDescriptorName.DIRECTIONAL_LIGHT] = VkDescriptor(descriptorSet, descriptorSetLayout)
    }
}