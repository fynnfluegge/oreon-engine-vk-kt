package org.oreon.core.vk.scenegraph

import org.lwjgl.vulkan.VK10
import org.oreon.core.math.Vec3f
import org.oreon.core.scenegraph.Camera
import org.oreon.core.util.BufferUtil
import org.oreon.core.vk.context.DeviceManager.DeviceType
import org.oreon.core.vk.context.VkContext.deviceManager
import org.oreon.core.vk.context.VkContext.resources
import org.oreon.core.vk.context.VkResources.VkDescriptorName
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import org.oreon.core.vk.wrapper.buffer.VkUniformBuffer
import org.oreon.core.vk.wrapper.descriptor.VkDescriptor

class VkCamera : Camera(Vec3f(-160f, 45f, -72f), Vec3f(0.5668308f, -0.028192917f, 0.82335174f),
        Vec3f(0.015936304f, 0.9996025f, 0.023256794f)) {
    private var uniformBuffer: VkUniformBuffer? = null
    lateinit var descriptorSet: DescriptorSet
    lateinit var descriptorSetLayout: DescriptorSetLayout
    override fun init() {
        val device = deviceManager.getLogicalDevice(DeviceType.MAJOR_GRAPHICS_DEVICE).handle
        uniformBuffer = VkUniformBuffer(
                device, deviceManager.getPhysicalDevice(DeviceType.MAJOR_GRAPHICS_DEVICE)
                .memoryProperties, BufferUtil.createByteBuffer(floatBuffer))
        descriptorSetLayout = DescriptorSetLayout(device, 1)
        descriptorSetLayout!!.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
                VK10.VK_SHADER_STAGE_ALL_GRAPHICS or VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorSetLayout!!.create()
        descriptorSet = DescriptorSet(device,
                deviceManager.getLogicalDevice(DeviceType.MAJOR_GRAPHICS_DEVICE)
                        .getDescriptorPool(Thread.currentThread().id)!!.handle,
                descriptorSetLayout.handlePointer)
        descriptorSet!!.updateDescriptorBuffer(uniformBuffer!!.handle, bufferSize.toLong(), 0, 0,
                VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
        resources.descriptors[VkDescriptorName.CAMERA] = VkDescriptor(descriptorSet, descriptorSetLayout)
    }

    override fun update() {
        super.update()
        uniformBuffer!!.updateData(BufferUtil.createByteBuffer(floatBuffer))
    }

    override fun shutdown() {
        uniformBuffer!!.destroy()
    }

    init {

        // flip y-axxis for vulkan coordinate system
        projectionMatrix[1, 1] = -projectionMatrix[1, 1]
    }
}