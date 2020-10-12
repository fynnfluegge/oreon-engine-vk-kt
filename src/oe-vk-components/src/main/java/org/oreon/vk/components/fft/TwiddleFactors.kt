package org.oreon.vk.components.fft

import org.oreon.core.util.Util.initBitReversedIndices
import org.oreon.core.vk.device.VkDeviceBundle
import org.oreon.core.vk.image.VkImageView
import org.oreon.core.vk.image.VkImage
import org.lwjgl.vulkan.VkDevice
import org.oreon.core.vk.memory.VkBuffer
import org.oreon.core.vk.wrapper.descriptor.VkDescriptor
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import org.lwjgl.vulkan.VK10
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.wrapper.image.Image2DDeviceLocal
import org.oreon.core.vk.wrapper.buffer.VkBufferHelper
import org.lwjgl.system.MemoryUtil
import org.oreon.core.util.BufferUtil
import org.oreon.core.vk.pipeline.ShaderModule
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.wrapper.command.ComputeCmdBuffer
import org.oreon.core.vk.util.VkUtil
import org.oreon.core.vk.synchronization.Fence
import org.oreon.core.vk.command.SubmitInfo
import org.oreon.core.vk.descriptor.DescriptorPool

class TwiddleFactors(deviceBundle: VkDeviceBundle, n: Int) {

    val imageView: VkImageView
    private val image: VkImage

    fun destroy() {
        image.destroy()
        imageView.destroy()
    }

    init {
        val device = deviceBundle.logicalDevice.handle
        val memoryProperties = deviceBundle.physicalDevice.memoryProperties
        val descriptorPool = deviceBundle.logicalDevice.getDescriptorPool(Thread.currentThread().id)
        val queue = deviceBundle.logicalDevice.computeQueue
        val log_2_n = (Math.log(n.toDouble()) / Math.log(2.0)).toInt()
        image = Image2DDeviceLocal(device, memoryProperties, log_2_n, n,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT,
                VK10.VK_IMAGE_USAGE_SAMPLED_BIT or VK10.VK_IMAGE_USAGE_STORAGE_BIT)
        imageView = VkImageView(device,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT, image.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        val bitReversedIndicesBuffer = VkBufferHelper.createDeviceLocalBuffer(device,
                memoryProperties,
                deviceBundle.logicalDevice.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                deviceBundle.logicalDevice.transferQueue,
                BufferUtil.createByteBuffer(*initBitReversedIndices(n)),
                VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT)
        val pushConstants = MemoryUtil.memAlloc(Integer.BYTES * 1)
        val intBuffer = pushConstants.asIntBuffer()
        intBuffer.put(n)
        val descriptor: VkDescriptor = TwiddleDescriptor(device, descriptorPool,
                imageView, bitReversedIndicesBuffer, n)
        val computeShader = ShaderModule(device,
                "shaders/fft/twiddleFactors.comp.spv", VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        val pipeline = VkPipeline(device)
        pipeline.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, Integer.BYTES * 1)
        pipeline.setLayout(descriptor.descriptorSetLayout.handlePointer)
        pipeline.createComputePipeline(computeShader)
        val commandBuffer: CommandBuffer = ComputeCmdBuffer(device,
                deviceBundle.logicalDevice.getComputeCommandPool(Thread.currentThread().id)!!.handle,
                pipeline.handle, pipeline.layoutHandle,
                VkUtil.createLongArray(descriptor.descriptorSet),
                log_2_n, n / 16, 1,
                pushConstants, VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        val fence = Fence(device)
        val submitInfo = SubmitInfo()
        submitInfo.setCommandBuffers(commandBuffer.handlePointer)
        submitInfo.fence = fence
        submitInfo.submit(queue)
        fence.waitForFence()
        computeShader.destroy()
        pipeline.destroy()
        commandBuffer.destroy()
        fence.destroy()
        descriptor.destroy()
        bitReversedIndicesBuffer.destroy()
        MemoryUtil.memFree(pushConstants)
    }

    private inner class TwiddleDescriptor(device: VkDevice?, descriptorPool: DescriptorPool?,
                                          imageView: VkImageView, buffer: VkBuffer, n: Int) : VkDescriptor() {
        init {
            descriptorSetLayout = DescriptorSetLayout(device!!, 2)
            descriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                    VK10.VK_SHADER_STAGE_COMPUTE_BIT)
            descriptorSetLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
                    VK10.VK_SHADER_STAGE_COMPUTE_BIT)
            descriptorSetLayout.create()

            descriptorSet = DescriptorSet(device, descriptorPool!!.handle,
                    descriptorSetLayout.handlePointer)
            descriptorSet.updateDescriptorImageBuffer(imageView.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1,
                    0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
            descriptorSet.updateDescriptorBuffer(buffer.handle, Integer.BYTES * n.toLong(), 0, 1,
                    VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
        }
    }
}