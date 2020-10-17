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
import org.oreon.core.vk.image.VkSampler
import org.oreon.core.vk.pipeline.ShaderModule
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.util.VkUtil.createLongArray
import org.oreon.core.vk.util.VkUtil.createLongBuffer
import org.oreon.core.vk.wrapper.image.Image2DDeviceLocal
import org.oreon.core.vk.wrapper.shader.ComputeShader
import java.nio.ByteBuffer
import java.util.*

class FXAA(deviceBundle: VkDeviceBundle, width: Int, height: Int,
           sceneImageView: VkImageView) {
    private val fxaaImage: VkImage
    var fxaaImageView: VkImageView
    private val computePipeline: VkPipeline
    private val descriptorSet: DescriptorSet
    private val descriptorSetLayout: DescriptorSetLayout
    private val sceneSampler: VkSampler
    private val pushConstants: ByteBuffer
    private val descriptorSets: MutableList<DescriptorSet>
    private val width: Int
    private val height: Int
    fun record(commandBuffer: CommandBuffer) {
        commandBuffer.pushConstantsCmd(computePipeline.layoutHandle,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstants)
        commandBuffer.bindComputePipelineCmd(computePipeline.handle)
        commandBuffer.bindComputeDescriptorSetsCmd(computePipeline.layoutHandle,
                createLongArray(descriptorSets))
        commandBuffer.dispatchCmd(width / 16, height / 16, 1)
    }

    fun shutdown() {
        fxaaImage.destroy()
        fxaaImageView.destroy()
        computePipeline.destroy()
        descriptorSet.destroy()
        descriptorSetLayout.destroy()
        sceneSampler.destroy()
    }

    init {
        val device = deviceBundle.logicalDevice.handle
        val memoryProperties = deviceBundle.physicalDevice.memoryProperties
        val descriptorPool = deviceBundle.logicalDevice.getDescriptorPool(Thread.currentThread().id)
        this.width = width
        this.height = height
        fxaaImage = Image2DDeviceLocal(device, memoryProperties,
                width, height, VK10.VK_FORMAT_R16G16B16A16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT
                or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        fxaaImageView = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, fxaaImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        sceneSampler = VkSampler(device, VK10.VK_FILTER_LINEAR, false, 0f,
                VK10.VK_SAMPLER_MIPMAP_MODE_NEAREST, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)
        val pushConstantRange = java.lang.Float.BYTES * 2
        pushConstants = MemoryUtil.memAlloc(pushConstantRange)
        pushConstants.putFloat(config.frameWidth.toFloat())
        pushConstants.putFloat(config.frameHeight.toFloat())
        pushConstants.flip()
        descriptorSetLayout = DescriptorSetLayout(device, 3)
        descriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorSetLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorSetLayout.create()
        descriptorSet = DescriptorSet(device,
                descriptorPool!!.handle, descriptorSetLayout.handlePointer)
        descriptorSet.updateDescriptorImageBuffer(fxaaImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        descriptorSet.updateDescriptorImageBuffer(sceneImageView.handle,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, sceneSampler.handle, 1,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSets = ArrayList()
        val descriptorSetLayouts: MutableList<DescriptorSetLayout> = ArrayList()
        descriptorSets.add(descriptorSet)
        descriptorSetLayouts.add(descriptorSetLayout)
        val shader: ShaderModule = ComputeShader(device, "shaders/fxaa.comp.spv")
        computePipeline = VkPipeline(device)
        computePipeline.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstantRange)
        computePipeline.setLayout(createLongBuffer(descriptorSetLayouts))
        computePipeline.createComputePipeline(shader)
        shader.destroy()
    }
}