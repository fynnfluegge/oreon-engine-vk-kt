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
    var bloomSceneImageBundle: VkImageBundle? = null
    private var additiveBlendImageBundle: VkImageBundle? = null
    private var sceneBrightnessImageBundle: VkImageBundle? = null
    private var sceneBrightnessImageBundle_div4: VkImageBundle? = null
    private var sceneBrightnessImageBundle_div8: VkImageBundle? = null
    private var sceneBrightnessImageBundle_div16: VkImageBundle? = null
    private var horizontalBloomBlurImageBundle_div2: VkImageBundle? = null
    private var horizontalBloomBlurImageBundle_div4: VkImageBundle? = null
    private var horizontalBloomBlurImageBundle_div8: VkImageBundle? = null
    private var horizontalBloomBlurImageBundle_div16: VkImageBundle? = null
    private var verticalBloomBlurImageBundle_div2: VkImageBundle? = null
    private var verticalBloomBlurImageBundle_div4: VkImageBundle? = null
    private var verticalBloomBlurImageBundle_div8: VkImageBundle? = null
    private var verticalBloomBlurImageBundle_div16: VkImageBundle? = null

    // scene brightness resources
    private val sceneBrightnessPipeline: VkPipeline
    private val sceneBrightnessDescriptorSet: DescriptorSet
    private val sceneBrightnessDescriptorSetLayout: DescriptorSetLayout
    private val sceneBrightnessDescriptorSets: MutableList<DescriptorSet>

    // horizontal blur resources
    private val horizontalBlurDescriptorSetLayout: DescriptorSetLayout
    private val horizontalBlurDescriptorSets: MutableList<DescriptorSet>
    private val horizontalBlurPipeline: VkPipeline
    private val horizontalBlurDescriptorSet: DescriptorSet

    // vertical blur resources
    private val verticalBlurPipeline: VkPipeline
    private val verticalBlurDescriptorSet: DescriptorSet
    private val verticalBlurDescriptorSetLayout: DescriptorSetLayout
    private val verticalBlurDescriptorSets: MutableList<DescriptorSet>

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
    private val pushConstants: ByteBuffer
    private val pushConstants_blend: ByteBuffer
    private val width: Int
    private val height: Int
    fun record(commandBuffer: CommandBuffer) {

        // scene luminance
        commandBuffer.pushConstantsCmd(sceneBrightnessPipeline.layoutHandle,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstants)
        commandBuffer.bindComputePipelineCmd(sceneBrightnessPipeline.handle)
        commandBuffer.bindComputeDescriptorSetsCmd(sceneBrightnessPipeline.layoutHandle,
                createLongArray(sceneBrightnessDescriptorSets))
        commandBuffer.dispatchCmd(width / 8, height / 8, 1)

        // barrier
        commandBuffer.pipelineMemoryBarrierCmd(
                VK10.VK_ACCESS_SHADER_WRITE_BIT, VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT)

        // horizontal blur
        commandBuffer.pushConstantsCmd(horizontalBlurPipeline.layoutHandle,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstants)
        commandBuffer.bindComputePipelineCmd(horizontalBlurPipeline.handle)
        commandBuffer.bindComputeDescriptorSetsCmd(horizontalBlurPipeline.layoutHandle,
                createLongArray(horizontalBlurDescriptorSets))
        commandBuffer.dispatchCmd(width / 16, height / 16, 1)

        // barrier
        commandBuffer.pipelineMemoryBarrierCmd(
                VK10.VK_ACCESS_SHADER_WRITE_BIT, VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT)

        // vertical blur
        commandBuffer.pushConstantsCmd(verticalBlurPipeline.layoutHandle,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstants)
        commandBuffer.bindComputePipelineCmd(verticalBlurPipeline.handle)
        commandBuffer.bindComputeDescriptorSetsCmd(verticalBlurPipeline.layoutHandle,
                createLongArray(verticalBlurDescriptorSets))
        commandBuffer.dispatchCmd(width / 16, height / 16, 1)

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
                width, height, VK10.VK_FORMAT_B8G8R8A8_SRGB, VK10.VK_IMAGE_USAGE_STORAGE_BIT
                or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val bloomSceneImageView = VkImageView(device!!,
                VK10.VK_FORMAT_B8G8R8A8_SRGB, bloomSceneImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        bloomSceneImageBundle = VkImageBundle(bloomSceneImage, bloomSceneImageView)
        val additiveBlendImage: VkImage = Image2DDeviceLocal(device, memoryProperties,
                width, height, VK10.VK_FORMAT_R16G16B16A16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT
                or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val additiveBlendImageView = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, additiveBlendImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        additiveBlendImageBundle = VkImageBundle(additiveBlendImage, additiveBlendImageView)

        // brightness images
        val sceneBrightnessImage: VkImage = Image2DDeviceLocal(device, memoryProperties,
                width, height, VK10.VK_FORMAT_R16G16B16A16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT
                or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val sceneBrightnessImageView = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, sceneBrightnessImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        val brightnessSampler = VkSampler(device, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        sceneBrightnessImageBundle = VkImageBundle(sceneBrightnessImage, sceneBrightnessImageView,
                brightnessSampler)
        val sceneBrightnessImage_div4: VkImage = Image2DDeviceLocal(device, memoryProperties,
                (width / 4.0f).toInt(), (height / 4.0f).toInt(), VK10.VK_FORMAT_R16G16B16A16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT
                or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val sceneBrightnessImageView_div4 = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, sceneBrightnessImage_div4.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        val brightnessSampler_div4 = VkSampler(device, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        sceneBrightnessImageBundle_div4 = VkImageBundle(sceneBrightnessImage_div4, sceneBrightnessImageView_div4,
                brightnessSampler_div4)
        val sceneBrightnessImage_div8: VkImage = Image2DDeviceLocal(device, memoryProperties,
                (width / 8.0f).toInt(), (height / 8.0f).toInt(), VK10.VK_FORMAT_R16G16B16A16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT
                or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val sceneBrightnessImageView_div8 = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, sceneBrightnessImage_div8.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        val brightnessSampler_div8 = VkSampler(device, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        sceneBrightnessImageBundle_div8 = VkImageBundle(sceneBrightnessImage_div8, sceneBrightnessImageView_div8,
                brightnessSampler_div8)
        val sceneBrightnessImage_div16: VkImage = Image2DDeviceLocal(device, memoryProperties,
                (width / 12.0f).toInt(), (height / 12.0f).toInt(), VK10.VK_FORMAT_R16G16B16A16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT
                or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val sceneBrightnessImageView_div16 = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, sceneBrightnessImage_div16.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        val brightnessSampler_div16 = VkSampler(device, VK10.VK_FILTER_LINEAR,
                false, 0f, VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        sceneBrightnessImageBundle_div16 = VkImageBundle(sceneBrightnessImage_div16, sceneBrightnessImageView_div16,
                brightnessSampler_div16)


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
                (width / 12.0f).toInt(), (height / 12.0f).toInt(), VK10.VK_FORMAT_R16G16B16A16_SFLOAT,
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
                (width / 12.0f).toInt(), (height / 12.0f).toInt(), VK10.VK_FORMAT_R16G16B16A16_SFLOAT,
                VK10.VK_IMAGE_USAGE_STORAGE_BIT or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        val verticalBloomBlurImageView_div16 = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, verticalBloomBlurImage_div16.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        verticalBloomBlurImageBundle_div16 = VkImageBundle(verticalBloomBlurImage_div16,
                verticalBloomBlurImageView_div16)
    }

    fun shutdown() {
        bloomSceneImageBundle!!.destroy()
        additiveBlendImageBundle!!.destroy()
        sceneBrightnessImageBundle!!.destroy()
        horizontalBloomBlurImageBundle_div2!!.destroy()
        horizontalBloomBlurImageBundle_div4!!.destroy()
        horizontalBloomBlurImageBundle_div8!!.destroy()
        horizontalBloomBlurImageBundle_div16!!.destroy()
        verticalBloomBlurImageBundle_div2!!.destroy()
        verticalBloomBlurImageBundle_div4!!.destroy()
        verticalBloomBlurImageBundle_div8!!.destroy()
        verticalBloomBlurImageBundle_div16!!.destroy()
        sceneBrightnessPipeline.destroy()
        sceneBrightnessDescriptorSet.destroy()
        sceneBrightnessDescriptorSetLayout.destroy()
        horizontalBlurPipeline.destroy()
        horizontalBlurDescriptorSet.destroy()
        horizontalBlurDescriptorSetLayout.destroy()
        verticalBlurPipeline.destroy()
        verticalBlurDescriptorSet.destroy()
        verticalBlurDescriptorSetLayout.destroy()
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
        pushConstants = MemoryUtil.memAlloc(java.lang.Float.BYTES * 12)
        pushConstants.putFloat(width / 2.0f)
        pushConstants.putFloat(height / 2.0f)
        pushConstants.putFloat(width / 4.0f)
        pushConstants.putFloat(height / 4.0f)
        pushConstants.putFloat(width / 8.0f)
        pushConstants.putFloat(height / 8.0f)
        pushConstants.putFloat(width / 12.0f)
        pushConstants.putFloat(height / 12.0f)
        pushConstants.putFloat(2f)
        pushConstants.putFloat(4f)
        pushConstants.putFloat(8f)
        pushConstants.putFloat(12f)
        pushConstants.flip()
        val pushConstantRange = java.lang.Float.BYTES * 2
        pushConstants_blend = MemoryUtil.memAlloc(pushConstantRange)
        pushConstants_blend.putFloat(width.toFloat())
        pushConstants_blend.putFloat(height.toFloat())
        pushConstants_blend.flip()


        // scene brightness
        sceneBrightnessDescriptorSetLayout = DescriptorSetLayout(device, 5)
        sceneBrightnessDescriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        sceneBrightnessDescriptorSetLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        sceneBrightnessDescriptorSetLayout.addLayoutBinding(2, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        sceneBrightnessDescriptorSetLayout.addLayoutBinding(3, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        sceneBrightnessDescriptorSetLayout.addLayoutBinding(4, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        sceneBrightnessDescriptorSetLayout.create()
        sceneBrightnessDescriptorSet = DescriptorSet(device, descriptorPool!!.handle,
                sceneBrightnessDescriptorSetLayout.handlePointer)
        sceneBrightnessDescriptorSet.updateDescriptorImageBuffer(
                sceneImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        sceneBrightnessDescriptorSet.updateDescriptorImageBuffer(
                sceneBrightnessImageBundle!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 1,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        sceneBrightnessDescriptorSet.updateDescriptorImageBuffer(
                sceneBrightnessImageBundle_div4!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 2,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        sceneBrightnessDescriptorSet.updateDescriptorImageBuffer(
                sceneBrightnessImageBundle_div8!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 3,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        sceneBrightnessDescriptorSet.updateDescriptorImageBuffer(
                sceneBrightnessImageBundle_div16!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 4,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        sceneBrightnessDescriptorSets = ArrayList()
        sceneBrightnessDescriptorSets.add(sceneBrightnessDescriptorSet)
        descriptorSetLayouts.add(sceneBrightnessDescriptorSetLayout)
        sceneBrightnessPipeline = VkPipeline(device)
        sceneBrightnessPipeline.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, java.lang.Float.BYTES * 12)
        sceneBrightnessPipeline.setLayout(createLongBuffer(descriptorSetLayouts))
        sceneBrightnessPipeline.createComputePipeline(sceneBrightnessShader)

        // horizontal blur
        horizontalBlurDescriptorSetLayout = DescriptorSetLayout(device, 8)
        horizontalBlurDescriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        horizontalBlurDescriptorSetLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        horizontalBlurDescriptorSetLayout.addLayoutBinding(2, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        horizontalBlurDescriptorSetLayout.addLayoutBinding(3, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        horizontalBlurDescriptorSetLayout.addLayoutBinding(4, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        horizontalBlurDescriptorSetLayout.addLayoutBinding(5, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        horizontalBlurDescriptorSetLayout.addLayoutBinding(6, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        horizontalBlurDescriptorSetLayout.addLayoutBinding(7, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        horizontalBlurDescriptorSetLayout.create()
        horizontalBlurDescriptorSet = DescriptorSet(device, descriptorPool.handle,
                horizontalBlurDescriptorSetLayout.handlePointer)
        horizontalBlurDescriptorSet.updateDescriptorImageBuffer(
                horizontalBloomBlurImageBundle_div2!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        horizontalBlurDescriptorSet.updateDescriptorImageBuffer(
                horizontalBloomBlurImageBundle_div4!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 1,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        horizontalBlurDescriptorSet.updateDescriptorImageBuffer(
                horizontalBloomBlurImageBundle_div8!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 2,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        horizontalBlurDescriptorSet.updateDescriptorImageBuffer(
                horizontalBloomBlurImageBundle_div16!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 3,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        horizontalBlurDescriptorSet.updateDescriptorImageBuffer(
                sceneBrightnessImageBundle!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, sceneBrightnessImageBundle!!.sampler.handle, 4,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        horizontalBlurDescriptorSet.updateDescriptorImageBuffer(
                sceneBrightnessImageBundle_div4!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, sceneBrightnessImageBundle_div4!!.sampler.handle, 5,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        horizontalBlurDescriptorSet.updateDescriptorImageBuffer(
                sceneBrightnessImageBundle_div8!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, sceneBrightnessImageBundle_div8!!.sampler.handle, 6,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        horizontalBlurDescriptorSet.updateDescriptorImageBuffer(
                sceneBrightnessImageBundle_div16!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, sceneBrightnessImageBundle_div16!!.sampler.handle, 7,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSetLayouts = ArrayList()
        horizontalBlurDescriptorSets = ArrayList()
        horizontalBlurDescriptorSets.add(horizontalBlurDescriptorSet)
        descriptorSetLayouts.add(horizontalBlurDescriptorSetLayout)
        horizontalBlurPipeline = VkPipeline(device)
        horizontalBlurPipeline.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, java.lang.Float.BYTES * 12)
        horizontalBlurPipeline.setLayout(createLongBuffer(descriptorSetLayouts))
        horizontalBlurPipeline.createComputePipeline(horizontalBlurShader)

        // vertical blur
        verticalBlurDescriptorSetLayout = DescriptorSetLayout(device, 8)
        verticalBlurDescriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        verticalBlurDescriptorSetLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        verticalBlurDescriptorSetLayout.addLayoutBinding(2, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        verticalBlurDescriptorSetLayout.addLayoutBinding(3, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        verticalBlurDescriptorSetLayout.addLayoutBinding(4, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        verticalBlurDescriptorSetLayout.addLayoutBinding(5, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        verticalBlurDescriptorSetLayout.addLayoutBinding(6, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        verticalBlurDescriptorSetLayout.addLayoutBinding(7, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        verticalBlurDescriptorSetLayout.create()
        verticalBlurDescriptorSet = DescriptorSet(device, descriptorPool.handle,
                verticalBlurDescriptorSetLayout.handlePointer)
        verticalBlurDescriptorSet.updateDescriptorImageBuffer(
                verticalBloomBlurImageBundle_div2!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        verticalBlurDescriptorSet.updateDescriptorImageBuffer(
                verticalBloomBlurImageBundle_div4!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 1,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        verticalBlurDescriptorSet.updateDescriptorImageBuffer(
                verticalBloomBlurImageBundle_div8!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 2,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        verticalBlurDescriptorSet.updateDescriptorImageBuffer(
                verticalBloomBlurImageBundle_div16!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 3,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        verticalBlurDescriptorSet.updateDescriptorImageBuffer(
                horizontalBloomBlurImageBundle_div2!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 4,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        verticalBlurDescriptorSet.updateDescriptorImageBuffer(
                horizontalBloomBlurImageBundle_div4!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 5,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        verticalBlurDescriptorSet.updateDescriptorImageBuffer(
                horizontalBloomBlurImageBundle_div8!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 6,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        verticalBlurDescriptorSet.updateDescriptorImageBuffer(
                horizontalBloomBlurImageBundle_div16!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 7,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        descriptorSetLayouts = ArrayList()
        verticalBlurDescriptorSets = ArrayList()
        verticalBlurDescriptorSets.add(verticalBlurDescriptorSet)
        descriptorSetLayouts.add(verticalBlurDescriptorSetLayout)
        verticalBlurPipeline = VkPipeline(device)
        verticalBlurPipeline.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, java.lang.Float.BYTES * 12)
        verticalBlurPipeline.setLayout(createLongBuffer(descriptorSetLayouts))
        verticalBlurPipeline.createComputePipeline(verticalBlurShader)

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
                additiveBlendImageBundle!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        blendDescriptorSet.updateDescriptorImageBuffer(
                verticalBloomBlurImageBundle_div2!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, bloomBlurSampler_div2.handle, 1,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        blendDescriptorSet.updateDescriptorImageBuffer(
                verticalBloomBlurImageBundle_div4!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, bloomBlurSampler_div4.handle, 2,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        blendDescriptorSet.updateDescriptorImageBuffer(
                verticalBloomBlurImageBundle_div8!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, bloomBlurSampler_div8.handle, 3,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        blendDescriptorSet.updateDescriptorImageBuffer(
                verticalBloomBlurImageBundle_div16!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, bloomBlurSampler_div16.handle, 4,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSetLayouts = ArrayList()
        blendDescriptorSets = ArrayList()
        blendDescriptorSets.add(blendDescriptorSet)
        descriptorSetLayouts.add(blendDescriptorSetLayout)
        blendPipeline = VkPipeline(device)
        blendPipeline.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstantRange)
        blendPipeline.setLayout(createLongBuffer(descriptorSetLayouts))
        blendPipeline.createComputePipeline(additiveBlendShader)

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
                additiveBlendImageBundle!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 1,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        bloomSceneDescriptorSet.updateDescriptorImageBuffer(
                specular_emission_bloom_attachment.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 2,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        bloomSceneDescriptorSet.updateDescriptorImageBuffer(
                bloomSceneImageBundle!!.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 3,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        descriptorSetLayouts = ArrayList()
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