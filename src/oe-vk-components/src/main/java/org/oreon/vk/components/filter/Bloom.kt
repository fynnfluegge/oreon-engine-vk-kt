package org.oreon.vk.components.filter

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VkQueue
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
import org.oreon.core.vk.wrapper.image.VkImageBundle
import org.oreon.core.vk.wrapper.shader.ComputeShader
import java.nio.ByteBuffer
import java.util.*

class Bloom(deviceBundle: VkDeviceBundle,
            width: Int, height: Int, sceneImageView: VkImageView, specular_emission_bloom_attachment: VkImageView) {
    private val queue: VkQueue?
    lateinit var bloomSceneImageBundle: VkImageBundle
        private set
    private lateinit var additiveBlendImageBundle: VkImageBundle
    private lateinit var sceneBrightnessImageBundle: VkImageBundle
    private lateinit var horizontalBloomBlurImageBundle_div2: VkImageBundle
    private lateinit var horizontalBloomBlurImageBundle_div4: VkImageBundle
    private lateinit var horizontalBloomBlurImageBundle_div8: VkImageBundle
    private lateinit var horizontalBloomBlurImageBundle_div16: VkImageBundle
    private lateinit var verticalBloomBlurImageBundle_div2: VkImageBundle
    private lateinit var verticalBloomBlurImageBundle_div4: VkImageBundle
    private lateinit var verticalBloomBlurImageBundle_div8: VkImageBundle
    private lateinit var verticalBloomBlurImageBundle_div16: VkImageBundle

    // scene brightness resources
    private val sceneBrightnessPipeline: VkPipeline
    private val sceneBrightnessDescriptorSet: DescriptorSet
    private val sceneBrightnessDescriptorSetLayout: DescriptorSetLayout
    private val sceneBrightnessDescriptorSets: MutableList<DescriptorSet>

    // horizontal DIV 2 blur resources
    private val horizontalBlurPipeline_div2: VkPipeline
    private val horizontalBlurDescriptorSet_div2: DescriptorSet
    private val horizontalBlurDescriptorSetLayout_div2: DescriptorSetLayout
    private val horizontalBlurDescriptorSets_div2: MutableList<DescriptorSet>

    // horizontal DIV 4 blur resources
    private val horizontalBlurPipeline_div4: VkPipeline
    private val horizontalBlurDescriptorSet_div4: DescriptorSet
    private val horizontalBlurDescriptorSetLayout_div4: DescriptorSetLayout
    private val horizontalBlurDescriptorSets_div4: MutableList<DescriptorSet>

    // horizontal DIV 8 blur resources
    private val horizontalBlurPipeline_div8: VkPipeline
    private val horizontalBlurDescriptorSet_div8: DescriptorSet
    private val horizontalBlurDescriptorSetLayout_div8: DescriptorSetLayout
    private val horizontalBlurDescriptorSets_div8: MutableList<DescriptorSet>

    // horizontal DIV 16 blur resources
    private val horizontalBlurPipeline_div16: VkPipeline
    private val horizontalBlurDescriptorSet_div16: DescriptorSet
    private val horizontalBlurDescriptorSetLayout_div16: DescriptorSetLayout
    private val horizontalBlurDescriptorSets_div16: MutableList<DescriptorSet>

    // vertical DIV 2 blur resources
    private val verticalBlurPipeline_div2: VkPipeline
    private val verticalBlurDescriptorSet_div2: DescriptorSet
    private val verticalBlurDescriptorSetLayout_div2: DescriptorSetLayout
    private val verticalBlurDescriptorSets_div2: MutableList<DescriptorSet>

    // vertical DIV 4 blur resources
    private val verticalBlurPipeline_div4: VkPipeline
    private val verticalBlurDescriptorSet_div4: DescriptorSet
    private val verticalBlurDescriptorSetLayout_div4: DescriptorSetLayout
    private val verticalBlurDescriptorSets_div4: MutableList<DescriptorSet>

    // vertical DIV 8 blur resources
    private val verticalBlurPipeline_div8: VkPipeline
    private val verticalBlurDescriptorSet_div8: DescriptorSet
    private val verticalBlurDescriptorSetLayout_div8: DescriptorSetLayout
    private val verticalBlurDescriptorSets_div8: MutableList<DescriptorSet>

    // vertical DIV 16 blur resources
    private val verticalBlurPipeline_div16: VkPipeline
    private val verticalBlurDescriptorSet_div16: DescriptorSet
    private val verticalBlurDescriptorSetLayout_div16: DescriptorSetLayout
    private val verticalBlurDescriptorSets_div16: MutableList<DescriptorSet>

    // blend resources
    private val blendPipeline: VkPipeline
    private val blendDescriptorSet: DescriptorSet
    private val blendDescriptorSetLayout: DescriptorSetLayout
    private val bloomBlurSampler_div2: VkSampler
    private val bloomBlurSampler_div4: VkSampler
    private val bloomBlurSampler_div8: VkSampler
    private val bloomBlurSampler_div16: VkSampler
    private val blendDescriptorSets: MutableList<DescriptorSet>

    // final bloom Scene resources
    private val bloomScenePipeline: VkPipeline
    private val bloomSceneDescriptorSet: DescriptorSet
    private val bloomSceneDescriptorSetLayout: DescriptorSetLayout
    private val bloomSceneDescriptorSets: MutableList<DescriptorSet>
    private val pushConstants_blend: ByteBuffer
    private val pushConstants_div2: ByteBuffer
    private val pushConstants_div4: ByteBuffer
    private val pushConstants_div8: ByteBuffer
    private val pushConstants_div16: ByteBuffer
    private val width: Int
    private val height: Int
    fun record(commandBuffer: CommandBuffer) {

        // scene luminance
        commandBuffer.bindComputePipelineCmd(sceneBrightnessPipeline.handle)
        commandBuffer.bindComputeDescriptorSetsCmd(sceneBrightnessPipeline.layoutHandle,
                createLongArray(sceneBrightnessDescriptorSets))
        commandBuffer.dispatchCmd(width / 8, height / 8, 1)

        // barrier
        commandBuffer.pipelineMemoryBarrierCmd(
                VK10.VK_ACCESS_SHADER_WRITE_BIT, VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT)

        // div2 horizontal blur
        commandBuffer.pushConstantsCmd(horizontalBlurPipeline_div2.layoutHandle,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstants_div2)
        commandBuffer.bindComputePipelineCmd(horizontalBlurPipeline_div2.handle)
        commandBuffer.bindComputeDescriptorSetsCmd(horizontalBlurPipeline_div2.layoutHandle,
                createLongArray(horizontalBlurDescriptorSets_div2))
        commandBuffer.dispatchCmd(width / 16, height / 16, 1)
        // div4 horizontal blur
        commandBuffer.pushConstantsCmd(horizontalBlurPipeline_div4.layoutHandle,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstants_div4)
        commandBuffer.bindComputePipelineCmd(horizontalBlurPipeline_div4.handle)
        commandBuffer.bindComputeDescriptorSetsCmd(horizontalBlurPipeline_div4.layoutHandle,
                createLongArray(horizontalBlurDescriptorSets_div4))
        commandBuffer.dispatchCmd(width / 32, height / 32, 1)
        // div8 horizontal blur
        commandBuffer.pushConstantsCmd(horizontalBlurPipeline_div8.layoutHandle,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstants_div8)
        commandBuffer.bindComputePipelineCmd(horizontalBlurPipeline_div8.handle)
        commandBuffer.bindComputeDescriptorSetsCmd(horizontalBlurPipeline_div8.layoutHandle,
                createLongArray(horizontalBlurDescriptorSets_div8))
        commandBuffer.dispatchCmd(width / 64, height / 64, 1)
        // div16 horizontal blur
        commandBuffer.pushConstantsCmd(horizontalBlurPipeline_div16.layoutHandle,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstants_div16)
        commandBuffer.bindComputePipelineCmd(horizontalBlurPipeline_div16.handle)
        commandBuffer.bindComputeDescriptorSetsCmd(horizontalBlurPipeline_div16.layoutHandle,
                createLongArray(horizontalBlurDescriptorSets_div16))
        commandBuffer.dispatchCmd(width / 128, height / 128, 1)

        // barrier
        commandBuffer.pipelineMemoryBarrierCmd(
                VK10.VK_ACCESS_SHADER_WRITE_BIT, VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT)

        // div2 vertical blur
        commandBuffer.pushConstantsCmd(verticalBlurPipeline_div2.layoutHandle,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstants_div2)
        commandBuffer.bindComputePipelineCmd(verticalBlurPipeline_div2.handle)
        commandBuffer.bindComputeDescriptorSetsCmd(verticalBlurPipeline_div2.layoutHandle,
                createLongArray(verticalBlurDescriptorSets_div2))
        commandBuffer.dispatchCmd(width / 16, height / 16, 1)
        // div4 horizontal blur
        commandBuffer.pushConstantsCmd(verticalBlurPipeline_div4.layoutHandle,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstants_div4)
        commandBuffer.bindComputePipelineCmd(verticalBlurPipeline_div4.handle)
        commandBuffer.bindComputeDescriptorSetsCmd(verticalBlurPipeline_div4.layoutHandle,
                createLongArray(verticalBlurDescriptorSets_div4))
        commandBuffer.dispatchCmd(width / 32, height / 32, 1)
        // div8 horizontal blur
        commandBuffer.pushConstantsCmd(verticalBlurPipeline_div8.layoutHandle,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstants_div8)
        commandBuffer.bindComputePipelineCmd(verticalBlurPipeline_div8.handle)
        commandBuffer.bindComputeDescriptorSetsCmd(verticalBlurPipeline_div8.layoutHandle,
                createLongArray(verticalBlurDescriptorSets_div8))
        commandBuffer.dispatchCmd(width / 64, height / 64, 1)
        // div16 horizontal blur
        commandBuffer.pushConstantsCmd(verticalBlurPipeline_div16.layoutHandle,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstants_div16)
        commandBuffer.bindComputePipelineCmd(verticalBlurPipeline_div16.handle)
        commandBuffer.bindComputeDescriptorSetsCmd(verticalBlurPipeline_div16.layoutHandle,
                createLongArray(verticalBlurDescriptorSets_div16))
        commandBuffer.dispatchCmd(width / 128, height / 128, 1)

        // barrier
        commandBuffer.pipelineMemoryBarrierCmd(
                VK10.VK_ACCESS_SHADER_WRITE_BIT, VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT)

        // additive blend
        commandBuffer.pushConstantsCmd(blendPipeline.layoutHandle,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstants_blend)
        commandBuffer.bindComputePipelineCmd(blendPipeline.handle)
        commandBuffer.bindComputeDescriptorSetsCmd(blendPipeline.layoutHandle,
                createLongArray(blendDescriptorSets))
        commandBuffer.dispatchCmd(width / 8, height / 8, 1)

        // barrier
        commandBuffer.pipelineMemoryBarrierCmd(
                VK10.VK_ACCESS_SHADER_WRITE_BIT, VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT)

        // bloom Scene
        commandBuffer.bindComputePipelineCmd(bloomScenePipeline.handle)
        commandBuffer.bindComputeDescriptorSetsCmd(bloomScenePipeline.layoutHandle,
                createLongArray(bloomSceneDescriptorSets))
        commandBuffer.dispatchCmd(width / 8, height / 8, 1)
    }

    fun initializeImages(device: VkDevice?,
                         memoryProperties: VkPhysicalDeviceMemoryProperties?, width: Int, height: Int) {
        val bloomSceneImage: VkImage = Image2DDeviceLocal(device, memoryProperties,
                width, height, VK10.VK_FORMAT_R16G16B16A16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT
                or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val bloomSceneImageView = VkImageView(device!!,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, bloomSceneImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        bloomSceneImageBundle = VkImageBundle(bloomSceneImage, bloomSceneImageView)
        val sceneBrightnessImage: VkImage = Image2DDeviceLocal(device, memoryProperties,
                width, height, VK10.VK_FORMAT_R16G16B16A16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT
                or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val sceneBrightnessImageView = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, sceneBrightnessImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        val brightnessSampler = VkSampler(device, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        sceneBrightnessImageBundle = VkImageBundle(sceneBrightnessImage, sceneBrightnessImageView,
                brightnessSampler)
        val additiveBlendImage: VkImage = Image2DDeviceLocal(device, memoryProperties,
                width, height, VK10.VK_FORMAT_R16G16B16A16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT
                or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val additiveBlendImageView = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, additiveBlendImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        additiveBlendImageBundle = VkImageBundle(additiveBlendImage, additiveBlendImageView)

        // horizontal Bloom Blur images
        val horizontalBloomBlurImage_div2: VkImage = Image2DDeviceLocal(device, memoryProperties,
                (width / 2.0f).toInt(), (height / 2.0f).toInt(), VK10.VK_FORMAT_R16G16B16A16_SFLOAT,
                VK10.VK_IMAGE_USAGE_STORAGE_BIT or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val horizontalBloomBlurImageView_div2 = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, horizontalBloomBlurImage_div2.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        horizontalBloomBlurImageBundle_div2 = VkImageBundle(horizontalBloomBlurImage_div2,
                horizontalBloomBlurImageView_div2)
        val horizontalBloomBlurImage_div4: VkImage = Image2DDeviceLocal(device, memoryProperties,
                (width / 4.0f).toInt(), (height / 4.0f).toInt(), VK10.VK_FORMAT_R16G16B16A16_SFLOAT,
                VK10.VK_IMAGE_USAGE_STORAGE_BIT or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val horizontalBloomBlurImageView_div4 = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, horizontalBloomBlurImage_div4.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        horizontalBloomBlurImageBundle_div4 = VkImageBundle(horizontalBloomBlurImage_div4,
                horizontalBloomBlurImageView_div4)
        val horizontalBloomBlurImage_div8: VkImage = Image2DDeviceLocal(device, memoryProperties,
                (width / 8.0f).toInt(), (height / 8.0f).toInt(), VK10.VK_FORMAT_R16G16B16A16_SFLOAT,
                VK10.VK_IMAGE_USAGE_STORAGE_BIT or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val horizontalBloomBlurImageView_div8 = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, horizontalBloomBlurImage_div8.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        horizontalBloomBlurImageBundle_div8 = VkImageBundle(horizontalBloomBlurImage_div8,
                horizontalBloomBlurImageView_div8)
        val horizontalBloomBlurImage_div16: VkImage = Image2DDeviceLocal(device, memoryProperties,
                (width / 16.0f).toInt(), (height / 16.0f).toInt(), VK10.VK_FORMAT_R16G16B16A16_SFLOAT,
                VK10.VK_IMAGE_USAGE_STORAGE_BIT or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val horizontalBloomBlurImageView_div16 = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, horizontalBloomBlurImage_div16.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        horizontalBloomBlurImageBundle_div16 = VkImageBundle(horizontalBloomBlurImage_div16,
                horizontalBloomBlurImageView_div16)

        // vertical Bloom Blur images
        val verticalBloomBlurImage_div2: VkImage = Image2DDeviceLocal(device, memoryProperties,
                (width / 2.0f).toInt(), (height / 2.0f).toInt(), VK10.VK_FORMAT_R16G16B16A16_SFLOAT,
                VK10.VK_IMAGE_USAGE_STORAGE_BIT or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val verticalBloomBlurImageView_div2 = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, verticalBloomBlurImage_div2.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        verticalBloomBlurImageBundle_div2 = VkImageBundle(verticalBloomBlurImage_div2,
                verticalBloomBlurImageView_div2)
        val verticalBloomBlurImage_div4: VkImage = Image2DDeviceLocal(device, memoryProperties,
                (width / 4.0f).toInt(), (height / 4.0f).toInt(), VK10.VK_FORMAT_R16G16B16A16_SFLOAT,
                VK10.VK_IMAGE_USAGE_STORAGE_BIT or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val verticalBloomBlurImageView_div4 = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, verticalBloomBlurImage_div4.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        verticalBloomBlurImageBundle_div4 = VkImageBundle(verticalBloomBlurImage_div4,
                verticalBloomBlurImageView_div4)
        val verticalBloomBlurImage_div8: VkImage = Image2DDeviceLocal(device, memoryProperties,
                (width / 8.0f).toInt(), (height / 8.0f).toInt(), VK10.VK_FORMAT_R16G16B16A16_SFLOAT,
                VK10.VK_IMAGE_USAGE_STORAGE_BIT or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val verticalBloomBlurImageView_div8 = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, verticalBloomBlurImage_div8.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        verticalBloomBlurImageBundle_div8 = VkImageBundle(verticalBloomBlurImage_div8,
                verticalBloomBlurImageView_div8)
        val verticalBloomBlurImage_div16: VkImage = Image2DDeviceLocal(device, memoryProperties,
                (width / 16.0f).toInt(), (height / 16.0f).toInt(), VK10.VK_FORMAT_R16G16B16A16_SFLOAT,
                VK10.VK_IMAGE_USAGE_STORAGE_BIT or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val verticalBloomBlurImageView_div16 = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, verticalBloomBlurImage_div16.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        verticalBloomBlurImageBundle_div16 = VkImageBundle(verticalBloomBlurImage_div16,
                verticalBloomBlurImageView_div16)
    }

    fun shutdown() {
        bloomSceneImageBundle.destroy()
        additiveBlendImageBundle.destroy()
        sceneBrightnessImageBundle.destroy()
        horizontalBloomBlurImageBundle_div2.destroy()
        horizontalBloomBlurImageBundle_div4.destroy()
        horizontalBloomBlurImageBundle_div8.destroy()
        horizontalBloomBlurImageBundle_div16.destroy()
        verticalBloomBlurImageBundle_div2.destroy()
        verticalBloomBlurImageBundle_div4.destroy()
        verticalBloomBlurImageBundle_div8.destroy()
        verticalBloomBlurImageBundle_div16.destroy()
        sceneBrightnessPipeline.destroy()
        sceneBrightnessDescriptorSet.destroy()
        sceneBrightnessDescriptorSetLayout.destroy()
        horizontalBlurPipeline_div2.destroy()
        horizontalBlurDescriptorSet_div2.destroy()
        horizontalBlurDescriptorSetLayout_div2.destroy()
        horizontalBlurPipeline_div4.destroy()
        horizontalBlurDescriptorSet_div4.destroy()
        horizontalBlurDescriptorSetLayout_div4.destroy()
        horizontalBlurPipeline_div8.destroy()
        horizontalBlurDescriptorSet_div8.destroy()
        horizontalBlurDescriptorSetLayout_div8.destroy()
        horizontalBlurPipeline_div16.destroy()
        horizontalBlurDescriptorSet_div16.destroy()
        horizontalBlurDescriptorSetLayout_div16.destroy()
        verticalBlurPipeline_div2.destroy()
        verticalBlurDescriptorSet_div2.destroy()
        verticalBlurDescriptorSetLayout_div2.destroy()
        verticalBlurPipeline_div4.destroy()
        verticalBlurDescriptorSet_div4.destroy()
        verticalBlurDescriptorSetLayout_div4.destroy()
        verticalBlurPipeline_div8.destroy()
        verticalBlurDescriptorSet_div8.destroy()
        verticalBlurDescriptorSetLayout_div8.destroy()
        verticalBlurPipeline_div16.destroy()
        verticalBlurDescriptorSet_div16.destroy()
        verticalBlurDescriptorSetLayout_div16.destroy()
        blendPipeline.destroy()
        blendDescriptorSet.destroy()
        blendDescriptorSetLayout.destroy()
        bloomBlurSampler_div2.destroy()
        bloomBlurSampler_div4.destroy()
        bloomBlurSampler_div8.destroy()
        bloomBlurSampler_div16.destroy()
        bloomScenePipeline.destroy()
        bloomSceneDescriptorSet.destroy()
        bloomSceneDescriptorSetLayout.destroy()
    }

    init {
        val device = deviceBundle.logicalDevice.handle
        val memoryProperties = deviceBundle.physicalDevice.memoryProperties
        val descriptorPool = deviceBundle.logicalDevice.getDescriptorPool(Thread.currentThread().id)
        queue = deviceBundle.logicalDevice.computeQueue
        this.width = width
        this.height = height
        initializeImages(device, memoryProperties, width, height)
        var descriptorSetLayouts: MutableList<DescriptorSetLayout> = ArrayList()
        val horizontalBlurShader: ShaderModule = ComputeShader(device,
                "shaders/filter/bloom/horizontalGaussianBlur.comp.spv")
        val verticalBlurShader: ShaderModule = ComputeShader(device,
                "shaders/filter/bloom/verticalGaussianBlur.comp.spv")
        val sceneBrightnessShader: ShaderModule = ComputeShader(device,
                "shaders/filter/bloom/sceneBrightness.comp.spv")
        val additiveBlendShader: ShaderModule = ComputeShader(device,
                "shaders/filter/bloom/additiveBlend.comp.spv")
        val bloomSceneShader: ShaderModule = ComputeShader(device,
                "shaders/filter/bloom/bloomScene.comp.spv")
        val pushConstantRange = java.lang.Float.BYTES * 2
        pushConstants_blend = MemoryUtil.memAlloc(pushConstantRange)
        pushConstants_blend.putFloat(width.toFloat())
        pushConstants_blend.putFloat(height.toFloat())
        pushConstants_blend.flip()
        pushConstants_div2 = MemoryUtil.memAlloc(pushConstantRange)
        pushConstants_div2.putFloat(width / 2.0f)
        pushConstants_div2.putFloat(height / 2.0f)
        pushConstants_div2.flip()
        pushConstants_div4 = MemoryUtil.memAlloc(pushConstantRange)
        pushConstants_div4.putFloat(width / 4.0f)
        pushConstants_div4.putFloat(height / 4.0f)
        pushConstants_div4.flip()
        pushConstants_div8 = MemoryUtil.memAlloc(pushConstantRange)
        pushConstants_div8.putFloat(width / 8.0f)
        pushConstants_div8.putFloat(height / 8.0f)
        pushConstants_div8.flip()
        pushConstants_div16 = MemoryUtil.memAlloc(pushConstantRange)
        pushConstants_div16.putFloat(width / 16.0f)
        pushConstants_div16.putFloat(height / 16.0f)
        pushConstants_div16.flip()

        // scene brightness
        sceneBrightnessDescriptorSetLayout = DescriptorSetLayout(device, 2)
        sceneBrightnessDescriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        sceneBrightnessDescriptorSetLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        sceneBrightnessDescriptorSetLayout.create()
        sceneBrightnessDescriptorSet = DescriptorSet(device, descriptorPool!!.handle,
                sceneBrightnessDescriptorSetLayout.handlePointer)
        sceneBrightnessDescriptorSet.updateDescriptorImageBuffer(
                sceneImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        sceneBrightnessDescriptorSet.updateDescriptorImageBuffer(
                sceneBrightnessImageBundle.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 1,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        sceneBrightnessDescriptorSets = ArrayList()
        sceneBrightnessDescriptorSets.add(sceneBrightnessDescriptorSet)
        descriptorSetLayouts.add(sceneBrightnessDescriptorSetLayout)
        sceneBrightnessPipeline = VkPipeline(device)
        sceneBrightnessPipeline.setLayout(createLongBuffer(descriptorSetLayouts))
        sceneBrightnessPipeline.createComputePipeline(sceneBrightnessShader)
        descriptorSetLayouts = ArrayList()

        // horizontal blur

        // DIV 2
        horizontalBlurDescriptorSetLayout_div2 = DescriptorSetLayout(device, 2)
        horizontalBlurDescriptorSetLayout_div2.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        horizontalBlurDescriptorSetLayout_div2.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        horizontalBlurDescriptorSetLayout_div2.create()
        horizontalBlurDescriptorSet_div2 = DescriptorSet(device, descriptorPool.handle,
                horizontalBlurDescriptorSetLayout_div2.handlePointer)
        horizontalBlurDescriptorSet_div2.updateDescriptorImageBuffer(
                horizontalBloomBlurImageBundle_div2.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        horizontalBlurDescriptorSet_div2.updateDescriptorImageBuffer(
                sceneBrightnessImageBundle.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, sceneBrightnessImageBundle.sampler.handle, 1,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        horizontalBlurDescriptorSets_div2 = ArrayList()
        horizontalBlurDescriptorSets_div2.add(horizontalBlurDescriptorSet_div2)
        descriptorSetLayouts.add(horizontalBlurDescriptorSetLayout_div2)
        horizontalBlurPipeline_div2 = VkPipeline(device)
        horizontalBlurPipeline_div2.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstantRange)
        horizontalBlurPipeline_div2.setLayout(createLongBuffer(descriptorSetLayouts))
        horizontalBlurPipeline_div2.createComputePipeline(horizontalBlurShader)
        descriptorSetLayouts = ArrayList()

        // DIV 4
        horizontalBlurDescriptorSetLayout_div4 = DescriptorSetLayout(device, 2)
        horizontalBlurDescriptorSetLayout_div4.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        horizontalBlurDescriptorSetLayout_div4.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        horizontalBlurDescriptorSetLayout_div4.create()
        horizontalBlurDescriptorSet_div4 = DescriptorSet(device, descriptorPool.handle,
                horizontalBlurDescriptorSetLayout_div4.handlePointer)
        horizontalBlurDescriptorSet_div4.updateDescriptorImageBuffer(
                horizontalBloomBlurImageBundle_div4.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        horizontalBlurDescriptorSet_div4.updateDescriptorImageBuffer(
                sceneBrightnessImageBundle.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, sceneBrightnessImageBundle.sampler.handle, 1,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        horizontalBlurDescriptorSets_div4 = ArrayList()
        horizontalBlurDescriptorSets_div4.add(horizontalBlurDescriptorSet_div4)
        descriptorSetLayouts.add(horizontalBlurDescriptorSetLayout_div4)
        horizontalBlurPipeline_div4 = VkPipeline(device)
        horizontalBlurPipeline_div4.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstantRange)
        horizontalBlurPipeline_div4.setLayout(createLongBuffer(descriptorSetLayouts))
        horizontalBlurPipeline_div4.createComputePipeline(horizontalBlurShader)
        descriptorSetLayouts = ArrayList()

        // DIV 8
        horizontalBlurDescriptorSetLayout_div8 = DescriptorSetLayout(device, 2)
        horizontalBlurDescriptorSetLayout_div8.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        horizontalBlurDescriptorSetLayout_div8.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        horizontalBlurDescriptorSetLayout_div8.create()
        horizontalBlurDescriptorSet_div8 = DescriptorSet(device, descriptorPool.handle,
                horizontalBlurDescriptorSetLayout_div8.handlePointer)
        horizontalBlurDescriptorSet_div8.updateDescriptorImageBuffer(
                horizontalBloomBlurImageBundle_div8.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        horizontalBlurDescriptorSet_div8.updateDescriptorImageBuffer(
                sceneBrightnessImageBundle.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, sceneBrightnessImageBundle.sampler.handle, 1,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        horizontalBlurDescriptorSets_div8 = ArrayList()
        horizontalBlurDescriptorSets_div8.add(horizontalBlurDescriptorSet_div8)
        descriptorSetLayouts.add(horizontalBlurDescriptorSetLayout_div8)
        horizontalBlurPipeline_div8 = VkPipeline(device)
        horizontalBlurPipeline_div8.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstantRange)
        horizontalBlurPipeline_div8.setLayout(createLongBuffer(descriptorSetLayouts))
        horizontalBlurPipeline_div8.createComputePipeline(horizontalBlurShader)
        descriptorSetLayouts = ArrayList()

        // DIV 16
        horizontalBlurDescriptorSetLayout_div16 = DescriptorSetLayout(device, 2)
        horizontalBlurDescriptorSetLayout_div16.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        horizontalBlurDescriptorSetLayout_div16.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        horizontalBlurDescriptorSetLayout_div16.create()
        horizontalBlurDescriptorSet_div16 = DescriptorSet(device, descriptorPool.handle,
                horizontalBlurDescriptorSetLayout_div16.handlePointer)
        horizontalBlurDescriptorSet_div16.updateDescriptorImageBuffer(
                horizontalBloomBlurImageBundle_div16.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        horizontalBlurDescriptorSet_div16.updateDescriptorImageBuffer(
                sceneBrightnessImageBundle.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, sceneBrightnessImageBundle.sampler.handle, 1,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        horizontalBlurDescriptorSets_div16 = ArrayList()
        horizontalBlurDescriptorSets_div16.add(horizontalBlurDescriptorSet_div16)
        descriptorSetLayouts.add(horizontalBlurDescriptorSetLayout_div16)
        horizontalBlurPipeline_div16 = VkPipeline(device)
        horizontalBlurPipeline_div16.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstantRange)
        horizontalBlurPipeline_div16.setLayout(createLongBuffer(descriptorSetLayouts))
        horizontalBlurPipeline_div16.createComputePipeline(horizontalBlurShader)
        descriptorSetLayouts = ArrayList()

        // vertical blur

        // DIV 2
        verticalBlurDescriptorSetLayout_div2 = DescriptorSetLayout(device, 2)
        verticalBlurDescriptorSetLayout_div2.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        verticalBlurDescriptorSetLayout_div2.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        verticalBlurDescriptorSetLayout_div2.create()
        verticalBlurDescriptorSet_div2 = DescriptorSet(device, descriptorPool.handle,
                verticalBlurDescriptorSetLayout_div2.handlePointer)
        verticalBlurDescriptorSet_div2.updateDescriptorImageBuffer(
                horizontalBloomBlurImageBundle_div2.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        verticalBlurDescriptorSet_div2.updateDescriptorImageBuffer(
                verticalBloomBlurImageBundle_div2.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 1,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        verticalBlurDescriptorSets_div2 = ArrayList()
        verticalBlurDescriptorSets_div2.add(verticalBlurDescriptorSet_div2)
        descriptorSetLayouts.add(verticalBlurDescriptorSetLayout_div2)
        verticalBlurPipeline_div2 = VkPipeline(device)
        verticalBlurPipeline_div2.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstantRange)
        verticalBlurPipeline_div2.setLayout(createLongBuffer(descriptorSetLayouts))
        verticalBlurPipeline_div2.createComputePipeline(verticalBlurShader)
        descriptorSetLayouts = ArrayList()

        // DIV 4
        verticalBlurDescriptorSetLayout_div4 = DescriptorSetLayout(device, 2)
        verticalBlurDescriptorSetLayout_div4.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        verticalBlurDescriptorSetLayout_div4.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        verticalBlurDescriptorSetLayout_div4.create()
        verticalBlurDescriptorSet_div4 = DescriptorSet(device, descriptorPool.handle,
                verticalBlurDescriptorSetLayout_div4.handlePointer)
        verticalBlurDescriptorSet_div4.updateDescriptorImageBuffer(
                horizontalBloomBlurImageBundle_div4.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        verticalBlurDescriptorSet_div4.updateDescriptorImageBuffer(
                verticalBloomBlurImageBundle_div4.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 1,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        verticalBlurDescriptorSets_div4 = ArrayList()
        verticalBlurDescriptorSets_div4.add(verticalBlurDescriptorSet_div4)
        descriptorSetLayouts.add(verticalBlurDescriptorSetLayout_div4)
        verticalBlurPipeline_div4 = VkPipeline(device)
        verticalBlurPipeline_div4.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstantRange)
        verticalBlurPipeline_div4.setLayout(createLongBuffer(descriptorSetLayouts))
        verticalBlurPipeline_div4.createComputePipeline(verticalBlurShader)
        descriptorSetLayouts = ArrayList()

        // DIV 8
        verticalBlurDescriptorSetLayout_div8 = DescriptorSetLayout(device, 2)
        verticalBlurDescriptorSetLayout_div8.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        verticalBlurDescriptorSetLayout_div8.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        verticalBlurDescriptorSetLayout_div8.create()
        verticalBlurDescriptorSet_div8 = DescriptorSet(device, descriptorPool.handle,
                verticalBlurDescriptorSetLayout_div8.handlePointer)
        verticalBlurDescriptorSet_div8.updateDescriptorImageBuffer(
                horizontalBloomBlurImageBundle_div8.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        verticalBlurDescriptorSet_div8.updateDescriptorImageBuffer(
                verticalBloomBlurImageBundle_div8.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 1,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        verticalBlurDescriptorSets_div8 = ArrayList()
        verticalBlurDescriptorSets_div8.add(verticalBlurDescriptorSet_div8)
        descriptorSetLayouts.add(verticalBlurDescriptorSetLayout_div8)
        verticalBlurPipeline_div8 = VkPipeline(device)
        verticalBlurPipeline_div8.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstantRange)
        verticalBlurPipeline_div8.setLayout(createLongBuffer(descriptorSetLayouts))
        verticalBlurPipeline_div8.createComputePipeline(verticalBlurShader)
        descriptorSetLayouts = ArrayList()

        // DIV 16
        verticalBlurDescriptorSetLayout_div16 = DescriptorSetLayout(device, 2)
        verticalBlurDescriptorSetLayout_div16.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        verticalBlurDescriptorSetLayout_div16.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        verticalBlurDescriptorSetLayout_div16.create()
        verticalBlurDescriptorSet_div16 = DescriptorSet(device, descriptorPool.handle,
                verticalBlurDescriptorSetLayout_div16.handlePointer)
        verticalBlurDescriptorSet_div16.updateDescriptorImageBuffer(
                horizontalBloomBlurImageBundle_div16.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        verticalBlurDescriptorSet_div16.updateDescriptorImageBuffer(
                verticalBloomBlurImageBundle_div16.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 1,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        verticalBlurDescriptorSets_div16 = ArrayList()
        verticalBlurDescriptorSets_div16.add(verticalBlurDescriptorSet_div16)
        descriptorSetLayouts.add(verticalBlurDescriptorSetLayout_div16)
        verticalBlurPipeline_div16 = VkPipeline(device)
        verticalBlurPipeline_div16.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstantRange)
        verticalBlurPipeline_div16.setLayout(createLongBuffer(descriptorSetLayouts))
        verticalBlurPipeline_div16.createComputePipeline(verticalBlurShader)
        descriptorSetLayouts = ArrayList()

        // aditive Blend
        bloomBlurSampler_div2 = VkSampler(device, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        bloomBlurSampler_div4 = VkSampler(device, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        bloomBlurSampler_div8 = VkSampler(device, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        bloomBlurSampler_div16 = VkSampler(device, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        blendDescriptorSetLayout = DescriptorSetLayout(device, 5)
        blendDescriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        blendDescriptorSetLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        blendDescriptorSetLayout.addLayoutBinding(2, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        blendDescriptorSetLayout.addLayoutBinding(3, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        blendDescriptorSetLayout.addLayoutBinding(4, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        blendDescriptorSetLayout.create()
        blendDescriptorSet = DescriptorSet(device, descriptorPool.handle,
                blendDescriptorSetLayout.handlePointer)
        blendDescriptorSet.updateDescriptorImageBuffer(
                additiveBlendImageBundle.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        blendDescriptorSet.updateDescriptorImageBuffer(
                verticalBloomBlurImageBundle_div2.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, bloomBlurSampler_div2.handle, 1,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        blendDescriptorSet.updateDescriptorImageBuffer(
                verticalBloomBlurImageBundle_div4.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, bloomBlurSampler_div4.handle, 2,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        blendDescriptorSet.updateDescriptorImageBuffer(
                verticalBloomBlurImageBundle_div8.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, bloomBlurSampler_div8.handle, 3,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        blendDescriptorSet.updateDescriptorImageBuffer(
                verticalBloomBlurImageBundle_div16.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, bloomBlurSampler_div16.handle, 4,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        blendDescriptorSets = ArrayList()
        blendDescriptorSets.add(blendDescriptorSet)
        descriptorSetLayouts.add(blendDescriptorSetLayout)
        blendPipeline = VkPipeline(device)
        blendPipeline.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstantRange)
        blendPipeline.setLayout(createLongBuffer(descriptorSetLayouts))
        blendPipeline.createComputePipeline(additiveBlendShader)
        descriptorSetLayouts = ArrayList()

        // final bloom scene
        bloomSceneDescriptorSetLayout = DescriptorSetLayout(device, 4)
        bloomSceneDescriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        bloomSceneDescriptorSetLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        bloomSceneDescriptorSetLayout.addLayoutBinding(2, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        bloomSceneDescriptorSetLayout.addLayoutBinding(3, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        bloomSceneDescriptorSetLayout.create()
        bloomSceneDescriptorSet = DescriptorSet(device, descriptorPool.handle,
                bloomSceneDescriptorSetLayout.handlePointer)
        bloomSceneDescriptorSet.updateDescriptorImageBuffer(
                sceneImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        bloomSceneDescriptorSet.updateDescriptorImageBuffer(
                additiveBlendImageBundle.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 1,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        bloomSceneDescriptorSet.updateDescriptorImageBuffer(
                specular_emission_bloom_attachment.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 2,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        bloomSceneDescriptorSet.updateDescriptorImageBuffer(
                bloomSceneImageBundle.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 3,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        bloomSceneDescriptorSets = ArrayList()
        bloomSceneDescriptorSets.add(bloomSceneDescriptorSet)
        descriptorSetLayouts.add(bloomSceneDescriptorSetLayout)
        bloomScenePipeline = VkPipeline(device)
        bloomScenePipeline.setLayout(createLongBuffer(descriptorSetLayouts))
        bloomScenePipeline.createComputePipeline(bloomSceneShader)
        horizontalBlurShader.destroy()
        verticalBlurShader.destroy()
        additiveBlendShader.destroy()
        sceneBrightnessShader.destroy()
        bloomSceneShader.destroy()
    }
}