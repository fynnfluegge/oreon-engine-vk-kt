package org.oreon.vk.components.fft

import lombok.Getter
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkQueue
import org.oreon.core.util.BufferUtil
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.command.SubmitInfo
import org.oreon.core.vk.descriptor.DescriptorPool
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import org.oreon.core.vk.device.VkDeviceBundle
import org.oreon.core.vk.image.VkImage
import org.oreon.core.vk.image.VkImageView
import org.oreon.core.vk.pipeline.ShaderModule
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.synchronization.VkSemaphore
import org.oreon.core.vk.util.VkUtil
import org.oreon.core.vk.wrapper.buffer.VkUniformBuffer
import org.oreon.core.vk.wrapper.command.ComputeCmdBuffer
import org.oreon.core.vk.wrapper.descriptor.VkDescriptor
import org.oreon.core.vk.wrapper.image.Image2DDeviceLocal

class Hkt(deviceBundle: VkDeviceBundle, N: Int, L: Int, private val t_delta: Float,
          tilde_h0k: VkImageView, tilde_h0minusk: VkImageView) {
    private val queue: VkQueue?

    @Getter
    private val dxCoefficients_imageView: VkImageView

    @Getter
    private val dyCoefficients_imageView: VkImageView

    @Getter
    private val dzCoefficients_imageView: VkImageView

    @Getter
    private val signalSemaphore: VkSemaphore
    private var t = 0f
    private var systemTime = System.currentTimeMillis()
    private val image_dxCoefficients: VkImage
    private val image_dyCoefficients: VkImage
    private val image_dzCoefficients: VkImage
    private val pipeline: VkPipeline
    private val descriptor: VkDescriptor
    private val buffer: VkUniformBuffer
    private val commandBuffer: CommandBuffer
    private val submitInfo: SubmitInfo

    private inner class CoefficientsDescriptor(device: VkDevice?, descriptorPool: DescriptorPool?,
                                               tilde_h0k: VkImageView, tilde_h0minusk: VkImageView) : VkDescriptor() {
        init {
            descriptorSetLayout = DescriptorSetLayout(device!!, 6)
            descriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                    VK10.VK_SHADER_STAGE_COMPUTE_BIT)
            descriptorSetLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                    VK10.VK_SHADER_STAGE_COMPUTE_BIT)
            descriptorSetLayout.addLayoutBinding(2, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                    VK10.VK_SHADER_STAGE_COMPUTE_BIT)
            descriptorSetLayout.addLayoutBinding(3, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                    VK10.VK_SHADER_STAGE_COMPUTE_BIT)
            descriptorSetLayout.addLayoutBinding(4, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                    VK10.VK_SHADER_STAGE_COMPUTE_BIT)
            descriptorSetLayout.addLayoutBinding(5, VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
                    VK10.VK_SHADER_STAGE_COMPUTE_BIT)
            descriptorSetLayout.create()
            descriptorSet = DescriptorSet(device, descriptorPool!!.handle,
                    descriptorSetLayout.handlePointer)
            descriptorSet.updateDescriptorImageBuffer(dyCoefficients_imageView.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1,
                    0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
            descriptorSet.updateDescriptorImageBuffer(dxCoefficients_imageView.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1,
                    1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
            descriptorSet.updateDescriptorImageBuffer(dzCoefficients_imageView.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1,
                    2, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
            descriptorSet.updateDescriptorImageBuffer(tilde_h0k.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1,
                    3, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
            descriptorSet.updateDescriptorImageBuffer(tilde_h0minusk.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1,
                    4, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
            descriptorSet.updateDescriptorBuffer(buffer.handle,
                    java.lang.Float.BYTES * 1.toLong(), 0, 5, VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
        }
    }

    fun render() {
        t += (System.currentTimeMillis() - systemTime) * t_delta
        val v = floatArrayOf(t)
        buffer.mapMemory(BufferUtil.createByteBuffer(*v))
        submitInfo.submit(queue)
        systemTime = System.currentTimeMillis()
    }

    fun destroy() {
        dxCoefficients_imageView.destroy()
        dyCoefficients_imageView.destroy()
        dzCoefficients_imageView.destroy()
        image_dxCoefficients.destroy()
        image_dyCoefficients.destroy()
        image_dzCoefficients.destroy()
        signalSemaphore.destroy()
        pipeline.destroy()
        descriptor.destroy()
        buffer.destroy()
        commandBuffer.destroy()
    }

    init {
        val device = deviceBundle.logicalDevice.handle
        val memoryProperties = deviceBundle.physicalDevice.memoryProperties
        val descriptorPool = deviceBundle.logicalDevice.getDescriptorPool(Thread.currentThread().id)
        queue = deviceBundle.logicalDevice.computeQueue
        image_dxCoefficients = Image2DDeviceLocal(device, memoryProperties, N, N,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT,
                VK10.VK_IMAGE_USAGE_SAMPLED_BIT or VK10.VK_IMAGE_USAGE_STORAGE_BIT)
        dxCoefficients_imageView = VkImageView(device,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT, image_dxCoefficients.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        image_dyCoefficients = Image2DDeviceLocal(device, memoryProperties, N, N,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT,
                VK10.VK_IMAGE_USAGE_SAMPLED_BIT or VK10.VK_IMAGE_USAGE_STORAGE_BIT)
        dyCoefficients_imageView = VkImageView(device,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT, image_dyCoefficients.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        image_dzCoefficients = Image2DDeviceLocal(device, memoryProperties, N, N,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT,
                VK10.VK_IMAGE_USAGE_SAMPLED_BIT or VK10.VK_IMAGE_USAGE_STORAGE_BIT)
        dzCoefficients_imageView = VkImageView(device,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT, image_dzCoefficients.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        val pushConstants = MemoryUtil.memAlloc(Integer.BYTES * 2)
        pushConstants.putInt(N)
        pushConstants.putInt(L)
        pushConstants.flip()
        val ubo = MemoryUtil.memAlloc(java.lang.Float.BYTES * 1)
        ubo.putFloat(t)
        ubo.flip()
        buffer = VkUniformBuffer(device, memoryProperties, ubo)
        descriptor = CoefficientsDescriptor(device, descriptorPool, tilde_h0k, tilde_h0minusk)
        pipeline = VkPipeline(device)
        pipeline.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, Integer.BYTES * 2)
        pipeline.setLayout(descriptor.descriptorSetLayout.handlePointer)
        pipeline.createComputePipeline(ShaderModule(device, "shaders/fft/hkt.comp.spv", VK10.VK_SHADER_STAGE_COMPUTE_BIT))
        commandBuffer = ComputeCmdBuffer(device,
                deviceBundle.logicalDevice.getComputeCommandPool(Thread.currentThread().id)!!.handle,
                pipeline.handle, pipeline.layoutHandle,
                VkUtil.createLongArray(descriptor.descriptorSet), N / 16, N / 16, 1,
                pushConstants, VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        signalSemaphore = VkSemaphore(device)
        submitInfo = SubmitInfo()
        submitInfo.setCommandBuffers(commandBuffer.handlePointer)
        submitInfo.setSignalSemaphores(signalSemaphore.handlePointer)
    }
}