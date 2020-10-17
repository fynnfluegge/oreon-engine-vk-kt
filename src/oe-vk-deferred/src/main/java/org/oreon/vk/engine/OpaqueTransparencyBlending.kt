package org.oreon.vk.engine

import lombok.Getter
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkQueue
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.command.SubmitInfo
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import org.oreon.core.vk.device.VkDeviceBundle
import org.oreon.core.vk.image.VkImage
import org.oreon.core.vk.image.VkImageView
import org.oreon.core.vk.image.VkSampler
import org.oreon.core.vk.pipeline.ShaderModule
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.synchronization.VkSemaphore
import org.oreon.core.vk.util.VkUtil.createLongArray
import org.oreon.core.vk.util.VkUtil.createLongBuffer
import org.oreon.core.vk.wrapper.command.ComputeCmdBuffer
import org.oreon.core.vk.wrapper.image.Image2DDeviceLocal
import org.oreon.core.vk.wrapper.shader.ComputeShader
import java.nio.LongBuffer
import java.util.*

class OpaqueTransparencyBlending(deviceBundle: VkDeviceBundle,
                                 width: Int, height: Int, opaqueSceneImageView: VkImageView,
                                 opaqueSceneDepthMap: VkImageView, opaqueSceneLightScatteringImageView: VkImageView,
                                 transparencySceneImageView: VkImageView, transparencySceneDepthMap: VkImageView,
                                 transparencyAlphaMap: VkImageView, transparencyLightScatteringImageView: VkImageView,
                                 waitSemaphores: LongBuffer?) {
    private val queue: VkQueue?
    private val blendedSceneImage: VkImage

    @Getter
    private val blendedSceneImageView: VkImageView
    private val blendedLightScatteringImage: VkImage

    @Getter
    private val blendedLightScatteringImageView: VkImageView
    private val computePipeline: VkPipeline
    private val descriptorSet: DescriptorSet
    private val descriptorSetLayout: DescriptorSetLayout
    private val cmdBuffer: CommandBuffer
    private val submitInfo: SubmitInfo

    // sampler
    private val opaqueSceneSampler: VkSampler
    private val opaqueSceneDepthSampler: VkSampler
    private val opaqueSceneLightScatteringSampler: VkSampler
    private val transparencySceneSampler: VkSampler
    private val transparencySceneDepthSampler: VkSampler
    private val transparencyAlphaSampler: VkSampler
    private val transparencyLightScatteringSampler: VkSampler
    var signalSemaphore: VkSemaphore
    fun render() {
        submitInfo.submit(queue)
    }

    fun shutdown() {
        computePipeline.destroy()
        descriptorSet.destroy()
        descriptorSetLayout.destroy()
        cmdBuffer.destroy()
        opaqueSceneSampler.destroy()
        opaqueSceneDepthSampler.destroy()
        opaqueSceneLightScatteringSampler.destroy()
        transparencySceneSampler.destroy()
        transparencySceneDepthSampler.destroy()
        transparencyAlphaSampler.destroy()
        transparencyLightScatteringSampler.destroy()
        signalSemaphore.destroy()
    }

    init {
        val device = deviceBundle.logicalDevice.handle
        val memoryProperties = deviceBundle.physicalDevice.memoryProperties
        val descriptorPool = deviceBundle.logicalDevice.getDescriptorPool(Thread.currentThread().id)
        queue = deviceBundle.logicalDevice.computeQueue
        blendedSceneImage = Image2DDeviceLocal(device, memoryProperties, width, height,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        blendedSceneImageView = VkImageView(device, blendedSceneImage.format,
                blendedSceneImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        blendedLightScatteringImage = Image2DDeviceLocal(device, memoryProperties, width, height,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        blendedLightScatteringImageView = VkImageView(device, blendedLightScatteringImage.format,
                blendedLightScatteringImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        opaqueSceneSampler = VkSampler(device, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT)
        opaqueSceneDepthSampler = VkSampler(device, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT)
        opaqueSceneLightScatteringSampler = VkSampler(device, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT)
        transparencySceneSampler = VkSampler(device, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT)
        transparencySceneDepthSampler = VkSampler(device, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT)
        transparencyAlphaSampler = VkSampler(device, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT)
        transparencyLightScatteringSampler = VkSampler(device, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT)
        descriptorSetLayout = DescriptorSetLayout(device, 9)
        descriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorSetLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorSetLayout.addLayoutBinding(2, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorSetLayout.addLayoutBinding(3, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorSetLayout.addLayoutBinding(4, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorSetLayout.addLayoutBinding(5, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorSetLayout.addLayoutBinding(6, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorSetLayout.addLayoutBinding(7, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorSetLayout.addLayoutBinding(8, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorSetLayout.create()
        descriptorSet = DescriptorSet(device, descriptorPool!!.handle,
                descriptorSetLayout.handlePointer)
        descriptorSet.updateDescriptorImageBuffer(blendedSceneImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        descriptorSet.updateDescriptorImageBuffer(blendedLightScatteringImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 1,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        descriptorSet.updateDescriptorImageBuffer(
                opaqueSceneImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, opaqueSceneSampler.handle,
                2, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSet.updateDescriptorImageBuffer(
                opaqueSceneLightScatteringImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, opaqueSceneLightScatteringSampler.handle,
                3, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSet.updateDescriptorImageBuffer(
                opaqueSceneDepthMap.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, opaqueSceneDepthSampler.handle,
                4, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSet.updateDescriptorImageBuffer(
                transparencySceneImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, transparencySceneSampler.handle,
                5, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSet.updateDescriptorImageBuffer(
                transparencyLightScatteringImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, transparencyLightScatteringSampler.handle,
                6, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSet.updateDescriptorImageBuffer(
                transparencySceneDepthMap.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, transparencySceneDepthSampler.handle,
                7, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSet.updateDescriptorImageBuffer(
                transparencyAlphaMap.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, transparencyAlphaSampler.handle,
                8, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        val descriptorSets: MutableList<DescriptorSet> = ArrayList()
        val descriptorSetLayouts: MutableList<DescriptorSetLayout> = ArrayList()
        descriptorSets.add(descriptorSet)
        descriptorSetLayouts.add(descriptorSetLayout)
        val pushConstantRange = java.lang.Float.BYTES * 2
        val pushConstants = MemoryUtil.memAlloc(pushConstantRange)
        pushConstants.putFloat(width.toFloat())
        pushConstants.putFloat(height.toFloat())
        pushConstants.flip()
        val shader: ShaderModule = ComputeShader(device, "shaders/opaqueTransparencyBlend.comp.spv")
        computePipeline = VkPipeline(device)
        computePipeline.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstantRange)
        computePipeline.setLayout(createLongBuffer(descriptorSetLayouts))
        computePipeline.createComputePipeline(shader)
        cmdBuffer = ComputeCmdBuffer(device,
                deviceBundle.logicalDevice.getComputeCommandPool(Thread.currentThread().id)!!.handle,
                computePipeline.handle, computePipeline.layoutHandle,
                createLongArray(descriptorSets), width / 16, height / 16, 1,
                pushConstants, VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        signalSemaphore = VkSemaphore(device)
        val pWaitDstStageMask = MemoryUtil.memAllocInt(2)
        pWaitDstStageMask.put(0, VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT)
        pWaitDstStageMask.put(1, VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT)
        submitInfo = SubmitInfo()
        submitInfo.setCommandBuffers(cmdBuffer.handlePointer)
        submitInfo.setWaitSemaphores(waitSemaphores!!)
        submitInfo.setWaitDstStageMask(pWaitDstStageMask)
        submitInfo.setSignalSemaphores(signalSemaphore.handlePointer)
        shader.destroy()
    }
}