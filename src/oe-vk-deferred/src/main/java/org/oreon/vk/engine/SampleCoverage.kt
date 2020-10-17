package org.oreon.vk.engine

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.oreon.core.context.BaseContext.Companion.config
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import org.oreon.core.vk.device.VkDeviceBundle
import org.oreon.core.vk.image.VkImage
import org.oreon.core.vk.image.VkImageView
import org.oreon.core.vk.pipeline.ShaderModule
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.util.VkUtil.createLongArray
import org.oreon.core.vk.util.VkUtil.createLongBuffer
import org.oreon.core.vk.wrapper.image.Image2DDeviceLocal
import org.oreon.core.vk.wrapper.shader.ComputeShader
import java.nio.ByteBuffer
import java.util.*

class SampleCoverage(deviceBundle: VkDeviceBundle,
                     width: Int, height: Int, worldPositionImageView: VkImageView,
                     lightScatteringMask: VkImageView, specularEmissionDiffuseSsaoBloomMask: VkImageView) {
    private val sampleCoverageImage: VkImage
    var lightScatteringImage: VkImage
    private val specularEmissionDiffuseSsaoBloomImage: VkImage
    var sampleCoverageImageView: VkImageView
    var lightScatteringImageView: VkImageView
    var specularEmissionDiffuseSsaoBloomImageView: VkImageView
    private val computePipeline: VkPipeline
    private val descriptorSet: DescriptorSet
    private val descriptorSetLayout: DescriptorSetLayout
    private val pushConstants: ByteBuffer
    private val descriptorSets: MutableList<DescriptorSet>
    private val width: Int
    private val height: Int
    private val discontinuitiestThreshold = 2f
    fun record(commandBuffer: CommandBuffer) {
        commandBuffer.pushConstantsCmd(computePipeline.layoutHandle,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstants)
        commandBuffer.bindComputePipelineCmd(computePipeline.handle)
        commandBuffer.bindComputeDescriptorSetsCmd(computePipeline.layoutHandle,
                createLongArray(descriptorSets))
        commandBuffer.dispatchCmd(width / 16, height / 16, 1)
    }

    fun shutdown() {
        sampleCoverageImage.destroy()
        sampleCoverageImageView.destroy()
        lightScatteringImage.destroy()
        lightScatteringImageView.destroy()
        computePipeline.destroy()
        descriptorSet.destroy()
        descriptorSetLayout.destroy()
    }

    init {
        val device = deviceBundle.logicalDevice.handle
        val memoryProperties = deviceBundle.physicalDevice.memoryProperties
        val descriptorPool = deviceBundle.logicalDevice.getDescriptorPool(Thread.currentThread().id)
        this.width = width
        this.height = height
        sampleCoverageImage = Image2DDeviceLocal(device, memoryProperties,
                width, height, VK10.VK_FORMAT_R16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT
                or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        sampleCoverageImageView = VkImageView(device,
                VK10.VK_FORMAT_R16_SFLOAT, sampleCoverageImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        lightScatteringImage = Image2DDeviceLocal(device, memoryProperties,
                width, height, VK10.VK_FORMAT_R16G16B16A16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT
                or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        lightScatteringImageView = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, lightScatteringImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        specularEmissionDiffuseSsaoBloomImage = Image2DDeviceLocal(device, memoryProperties,
                width, height, VK10.VK_FORMAT_R16G16B16A16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT
                or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        specularEmissionDiffuseSsaoBloomImageView = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, specularEmissionDiffuseSsaoBloomImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        descriptorSetLayout = DescriptorSetLayout(device, 6)
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
        descriptorSet.updateDescriptorImageBuffer(sampleCoverageImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        descriptorSet.updateDescriptorImageBuffer(worldPositionImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 1,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        descriptorSet.updateDescriptorImageBuffer(lightScatteringImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 2,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        descriptorSet.updateDescriptorImageBuffer(lightScatteringMask.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 3,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        descriptorSet.updateDescriptorImageBuffer(specularEmissionDiffuseSsaoBloomImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 4,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        descriptorSet.updateDescriptorImageBuffer(specularEmissionDiffuseSsaoBloomMask.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 5,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        descriptorSets = ArrayList()
        val descriptorSetLayouts: MutableList<DescriptorSetLayout> = ArrayList()
        descriptorSets.add(descriptorSet)
        descriptorSetLayouts.add(descriptorSetLayout)
        val pushConstantRange = java.lang.Float.BYTES * 1 + Integer.BYTES * 1
        pushConstants = MemoryUtil.memAlloc(pushConstantRange)
        pushConstants.putInt(config.multisampling_sampleCount)
        pushConstants.putFloat(discontinuitiestThreshold)
        pushConstants.flip()
        val shader: ShaderModule = ComputeShader(device, "shaders/sampleCoverage.comp.spv")
        computePipeline = VkPipeline(device)
        computePipeline.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstantRange)
        computePipeline.setLayout(createLongBuffer(descriptorSetLayouts))
        computePipeline.createComputePipeline(shader)
        shader.destroy()
    }
}