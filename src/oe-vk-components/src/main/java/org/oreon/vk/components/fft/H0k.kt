package org.oreon.vk.components.fft

import lombok.Getter
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.oreon.core.math.Vec2f
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.command.SubmitInfo
import org.oreon.core.vk.descriptor.DescriptorPool
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import org.oreon.core.vk.device.VkDeviceBundle
import org.oreon.core.vk.image.VkImage
import org.oreon.core.vk.image.VkImageView
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.synchronization.Fence
import org.oreon.core.vk.util.VkUtil
import org.oreon.core.vk.wrapper.command.ComputeCmdBuffer
import org.oreon.core.vk.wrapper.descriptor.VkDescriptor
import org.oreon.core.vk.wrapper.image.Image2DDeviceLocal
import org.oreon.core.vk.wrapper.image.VkImageHelper
import org.oreon.core.vk.wrapper.shader.ComputeShader

class H0k(deviceBundle: VkDeviceBundle, N: Int, L: Int,
          amplitude: Float, windDirection: Vec2f, windSpeed: Float, capillarSuppressFactor: Float) {
    @Getter
    private val h0k_imageView: VkImageView

    @Getter
    private val h0minusk_imageView: VkImageView
    private val h0k_image: VkImage
    private val h0minusk_image: VkImage

    private inner class SpectrumDescriptor(device: VkDevice?, descriptorPool: DescriptorPool?,
                                           noise0: VkImageView, noise1: VkImageView,
                                           noise2: VkImageView, noise3: VkImageView) : VkDescriptor() {
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
            descriptorSetLayout.addLayoutBinding(5, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                    VK10.VK_SHADER_STAGE_COMPUTE_BIT)
            descriptorSetLayout.create()
            descriptorSet = DescriptorSet(device, descriptorPool!!.handle,
                    descriptorSetLayout.handlePointer)
            descriptorSet.updateDescriptorImageBuffer(h0k_imageView.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
            descriptorSet.updateDescriptorImageBuffer(h0minusk_imageView.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
            descriptorSet.updateDescriptorImageBuffer(noise0.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 2, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
            descriptorSet.updateDescriptorImageBuffer(noise1.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 3, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
            descriptorSet.updateDescriptorImageBuffer(noise2.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 4, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
            descriptorSet.updateDescriptorImageBuffer(noise3.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 5, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        }
    }

    fun destroy() {
        h0k_image.destroy()
        h0k_imageView.destroy()
        h0minusk_image.destroy()
        h0minusk_imageView.destroy()
    }

    init {
        val device = deviceBundle.logicalDevice.handle
        val memoryProperties = deviceBundle.physicalDevice.memoryProperties
        val descriptorPool = deviceBundle.logicalDevice.getDescriptorPool(Thread.currentThread().id)
        val queue = deviceBundle.logicalDevice.computeQueue
        h0k_image = Image2DDeviceLocal(device, memoryProperties, N, N,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT)
        h0k_imageView = VkImageView(device,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT, h0k_image.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        h0minusk_image = Image2DDeviceLocal(device, memoryProperties, N, N,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT)
        h0minusk_imageView = VkImageView(device,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT, h0minusk_image.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        val noise0Image = VkImageHelper.loadImageFromFile(
                device, memoryProperties,
                deviceBundle.logicalDevice.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                deviceBundle.logicalDevice.transferQueue,
                "textures/noise/Noise" + N + "_0.jpg",
                VK10.VK_IMAGE_USAGE_STORAGE_BIT,
                VK10.VK_IMAGE_LAYOUT_GENERAL,
                VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_QUEUE_FAMILY_IGNORED)
        val noise0ImageView = VkImageView(device,
                VK10.VK_FORMAT_R8G8B8A8_UNORM, noise0Image.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        val noise1Image = VkImageHelper.loadImageFromFile(
                device, memoryProperties,
                deviceBundle.logicalDevice.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                deviceBundle.logicalDevice.transferQueue,
                "textures/noise/Noise" + N + "_1.jpg",
                VK10.VK_IMAGE_USAGE_STORAGE_BIT,
                VK10.VK_IMAGE_LAYOUT_GENERAL,
                VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_QUEUE_FAMILY_IGNORED)
        val noise1ImageView = VkImageView(device,
                VK10.VK_FORMAT_R8G8B8A8_UNORM, noise1Image.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        val noise2Image = VkImageHelper.loadImageFromFile(
                device, memoryProperties,
                deviceBundle.logicalDevice.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                deviceBundle.logicalDevice.transferQueue,
                "textures/noise/Noise" + N + "_2.jpg",
                VK10.VK_IMAGE_USAGE_STORAGE_BIT,
                VK10.VK_IMAGE_LAYOUT_GENERAL,
                VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_QUEUE_FAMILY_IGNORED)
        val noise2ImageView = VkImageView(device,
                VK10.VK_FORMAT_R8G8B8A8_UNORM, noise2Image.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        val noise3Image = VkImageHelper.loadImageFromFile(
                device, memoryProperties,
                deviceBundle.logicalDevice.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                deviceBundle.logicalDevice.transferQueue,
                "textures/noise/Noise" + N + "_3.jpg",
                VK10.VK_IMAGE_USAGE_STORAGE_BIT,
                VK10.VK_IMAGE_LAYOUT_GENERAL,
                VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_QUEUE_FAMILY_IGNORED)
        val noise3ImageView = VkImageView(device,
                VK10.VK_FORMAT_R8G8B8A8_UNORM, noise3Image.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        val pushConstantRange = Integer.BYTES * 2 + java.lang.Float.BYTES * 6
        val pushConstants = MemoryUtil.memAlloc(pushConstantRange)
        pushConstants.putInt(N)
        pushConstants.putInt(L)
        pushConstants.putFloat(amplitude)
        pushConstants.putFloat(windSpeed)
        pushConstants.putFloat(windDirection.x)
        pushConstants.putFloat(windDirection.y)
        pushConstants.putFloat(capillarSuppressFactor)
        pushConstants.putFloat(0f)
        pushConstants.flip()
        val descriptor: VkDescriptor = SpectrumDescriptor(device, descriptorPool,
                noise0ImageView, noise1ImageView, noise2ImageView, noise3ImageView)
        val pipeline = VkPipeline(device)
        pipeline.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstantRange)
        pipeline.setLayout(descriptor.descriptorSetLayout.handlePointer)
        pipeline.createComputePipeline(ComputeShader(device, "shaders/fft/h0k.comp.spv"))
        val commandBuffer: CommandBuffer = ComputeCmdBuffer(device,
                deviceBundle.logicalDevice.getComputeCommandPool(Thread.currentThread().id)!!.handle,
                pipeline.handle, pipeline.layoutHandle,
                VkUtil.createLongArray(descriptor.descriptorSet), N / 16, N / 16, 1,
                pushConstants, VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        val fence = Fence(device)
        val submitInfo = SubmitInfo()
        submitInfo.setCommandBuffers(commandBuffer.handlePointer)
        submitInfo.fence = fence
        submitInfo.submit(queue)
        fence.waitForFence()
        pipeline.destroy()
        commandBuffer.destroy()
        fence.destroy()
        descriptor.destroy()
        MemoryUtil.memFree(pushConstants)
        noise0Image.destroy()
        noise1Image.destroy()
        noise2Image.destroy()
        noise3Image.destroy()
        noise0ImageView.destroy()
        noise1ImageView.destroy()
        noise2ImageView.destroy()
        noise3ImageView.destroy()
    }
}