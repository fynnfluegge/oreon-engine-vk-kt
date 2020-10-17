package org.oreon.vk.components.util

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkQueue
import org.oreon.core.util.Util.getLog2N
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.command.CommandPool
import org.oreon.core.vk.command.SubmitInfo
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import org.oreon.core.vk.device.VkDeviceBundle
import org.oreon.core.vk.image.VkImage
import org.oreon.core.vk.image.VkImageView
import org.oreon.core.vk.image.VkSampler
import org.oreon.core.vk.pipeline.ShaderModule
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.synchronization.Fence
import org.oreon.core.vk.util.VkUtil.createLongArray
import org.oreon.core.vk.wrapper.command.ComputeCmdBuffer
import org.oreon.core.vk.wrapper.command.MipMapGenerationCmdBuffer
import org.oreon.core.vk.wrapper.image.Image2DDeviceLocal
import java.nio.LongBuffer

class NormalRenderer(deviceBundle: VkDeviceBundle, private val N: Int, strength: Float,
                     heightImageView: VkImageView, heightSampler: VkSampler) {
    private val commandBuffer: CommandBuffer
    private val pipeline: VkPipeline
    private val descriptorSet: DescriptorSet
    private val descriptorSetLayout: DescriptorSetLayout
    private val submitInfo: SubmitInfo
    private val normalImage: VkImage
    private val fence: Fence
    private val mipmapCmdBuffer: CommandBuffer
    private val mipmapSubmitInfo: SubmitInfo
    private val device: VkDevice
    private val computeQueue: VkQueue?
    private val transferQueue: VkQueue
    private val graphicsCommandPool: CommandPool?
    var normalImageView: VkImageView
    fun setWaitSemaphores(waitSemaphore: LongBuffer?) {
        val pWaitDstStageMask = MemoryUtil.memAllocInt(1)
        pWaitDstStageMask.put(0, VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT)
        submitInfo.setWaitDstStageMask(pWaitDstStageMask)
        submitInfo.setWaitSemaphores(waitSemaphore!!)
    }

    fun render(dstQueueFamilyIndex: Int) {
        submitInfo.submit(computeQueue)
        fence.waitForFence()
        mipmapSubmitInfo.submit(transferQueue)
    }

    fun destroy() {
        commandBuffer.destroy()
        pipeline.destroy()
        descriptorSet.destroy()
        descriptorSetLayout.destroy()
        fence.destroy()
        mipmapCmdBuffer.destroy()
        normalImageView.destroy()
        normalImage.destroy()
    }

    init {
        device = deviceBundle.logicalDevice.handle
        computeQueue = deviceBundle.logicalDevice.computeQueue
        transferQueue = deviceBundle.logicalDevice.graphicsQueue
        graphicsCommandPool = deviceBundle.logicalDevice.getGraphicsCommandPool(Thread.currentThread().id)
        normalImage = Image2DDeviceLocal(deviceBundle.logicalDevice.handle,
                deviceBundle.physicalDevice.memoryProperties, N, N,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT,
                VK10.VK_IMAGE_USAGE_SAMPLED_BIT or VK10.VK_IMAGE_USAGE_STORAGE_BIT or
                        VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
                1, getLog2N(N))
        normalImageView = VkImageView(deviceBundle.logicalDevice.handle,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT, normalImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT,
                getLog2N(N))
        descriptorSetLayout = DescriptorSetLayout(deviceBundle.logicalDevice.handle, 2)
        descriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorSetLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorSetLayout.create()
        descriptorSet = DescriptorSet(deviceBundle.logicalDevice.handle,
                deviceBundle.logicalDevice.getDescriptorPool(Thread.currentThread().id)!!.handle,
                descriptorSetLayout.handlePointer)
        descriptorSet.updateDescriptorImageBuffer(normalImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1,
                0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        descriptorSet.updateDescriptorImageBuffer(heightImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, heightSampler.handle,
                1, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        val pushConstants = MemoryUtil.memAlloc(Integer.BYTES + java.lang.Float.BYTES)
        pushConstants.putInt(N)
        pushConstants.putFloat(strength)
        pushConstants.flip()
        pipeline = VkPipeline(deviceBundle.logicalDevice.handle)
        pipeline.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, Integer.BYTES + java.lang.Float.BYTES)
        pipeline.setLayout(descriptorSetLayout.handlePointer)
        pipeline.createComputePipeline(ShaderModule(deviceBundle.logicalDevice.handle,
                "shaders/util/normals.comp.spv", VK10.VK_SHADER_STAGE_COMPUTE_BIT))
        commandBuffer = ComputeCmdBuffer(deviceBundle.logicalDevice.handle,
                deviceBundle.logicalDevice.getComputeCommandPool(Thread.currentThread().id)!!.handle,
                pipeline.handle, pipeline.layoutHandle,
                createLongArray(descriptorSet), N / 16, N / 16, 1,
                pushConstants, VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        fence = Fence(deviceBundle.logicalDevice.handle)
        submitInfo = SubmitInfo()
        submitInfo.fence = fence
        submitInfo.setCommandBuffers(commandBuffer.handlePointer)
        mipmapCmdBuffer = MipMapGenerationCmdBuffer(device,
                graphicsCommandPool!!.handle, normalImage.handle,
                N, N, getLog2N(N),
                VK10.VK_IMAGE_LAYOUT_UNDEFINED, 0, VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_IMAGE_LAYOUT_GENERAL, VK10.VK_ACCESS_SHADER_READ_BIT, VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
        mipmapSubmitInfo = SubmitInfo()
        mipmapSubmitInfo.setCommandBuffers(mipmapCmdBuffer.handlePointer)
    }
}