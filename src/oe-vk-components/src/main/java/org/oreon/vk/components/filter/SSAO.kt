package org.oreon.vk.components.filter

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VkQueue
import org.oreon.core.context.BaseContext.Companion.camera
import org.oreon.core.math.Vec4f
import org.oreon.core.util.BufferUtil.createByteBuffer
import org.oreon.core.util.Util.generateRandomKernel4D
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.command.CommandPool
import org.oreon.core.vk.command.SubmitInfo
import org.oreon.core.vk.context.VkContext.getCamera
import org.oreon.core.vk.descriptor.DescriptorPool
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import org.oreon.core.vk.device.VkDeviceBundle
import org.oreon.core.vk.image.VkImage
import org.oreon.core.vk.image.VkImageView
import org.oreon.core.vk.image.VkSampler
import org.oreon.core.vk.memory.VkBuffer
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.synchronization.Fence
import org.oreon.core.vk.util.VkUtil.createLongArray
import org.oreon.core.vk.util.VkUtil.createLongBuffer
import org.oreon.core.vk.wrapper.buffer.VkBufferHelper.createDeviceLocalBuffer
import org.oreon.core.vk.wrapper.command.ComputeCmdBuffer
import org.oreon.core.vk.wrapper.image.Image2DDeviceLocal
import org.oreon.core.vk.wrapper.shader.ComputeShader
import java.util.*

class SSAO(deviceBundle: VkDeviceBundle, width: Int, height: Int,
           worldPositionImageView: VkImageView, normalImageView: VkImageView,
           depthImageView: VkImageView) {
    private val queue: VkQueue?
    private val kernelSize: Int
    private val kernel: Array<Vec4f>
    private val randomx: FloatArray
    private val randomy: FloatArray
    private var noiseImageView: VkImageView? = null
    private lateinit var noiseImage: VkImage
    private var fence: Fence

    // ssao resources
    private val ssaoImage: VkImage

    var ssaoImageView: VkImageView
        private set
    private val ssaoPipeline: VkPipeline
    private val kernelBuffer: VkBuffer
    private val ssaoDescriptorSet: DescriptorSet
    private val ssaoDescriptorSetLayout: DescriptorSetLayout
    private val ssaoCmdBuffer: CommandBuffer
    private val ssaoSubmitInfo: SubmitInfo

    // ssao blur resources
    private val ssaoBlurSceneImage: VkImage

    var ssaoBlurSceneImageView: VkImageView
        private set
    private val ssaoBlurPipeline: VkPipeline
    private val ssaoBlurDescriptorSet: DescriptorSet
    private val ssaoBlurDescriptorSetLayout: DescriptorSetLayout
    private val ssaoBlurCmdBuffer: CommandBuffer
    private val ssaoBlurSubmitInfo: SubmitInfo
    private fun generateNoise(device: VkDevice,
                              memoryProperties: VkPhysicalDeviceMemoryProperties,
                              descriptorPool: DescriptorPool?, commandPool: CommandPool?) {
        noiseImage = Image2DDeviceLocal(device, memoryProperties,
                4, 4, VK10.VK_FORMAT_R16G16B16A16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT)
        noiseImageView = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, noiseImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        val pushConstantRange = java.lang.Float.BYTES * 32
        val pushConstants = MemoryUtil.memAlloc(pushConstantRange)
        for (i in randomx.indices) {
            pushConstants.putFloat(randomx[i])
        }
        for (i in randomy.indices) {
            pushConstants.putFloat(randomy[i])
        }
        pushConstants.flip()
        val descriptorSetLayout = DescriptorSetLayout(device, 1)
        descriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorSetLayout.create()
        val descriptorSet = DescriptorSet(device, descriptorPool!!.handle,
                descriptorSetLayout.handlePointer)
        descriptorSet.updateDescriptorImageBuffer(noiseImageView!!.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1,
                0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        val pipeline = VkPipeline(device)
        pipeline.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstantRange)
        pipeline.setLayout(descriptorSetLayout.handlePointer)
        pipeline.createComputePipeline(ComputeShader(device, "shaders/filter/ssao/noise.comp.spv"))
        val commandBuffer: CommandBuffer = ComputeCmdBuffer(device,
                commandPool!!.handle,
                pipeline.handle, pipeline.layoutHandle,
                createLongArray(descriptorSet), 1, 1, 1,
                pushConstants, VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        val fence = Fence(device)
        val submitInfo = SubmitInfo()
        submitInfo.setCommandBuffers(commandBuffer.handlePointer)
        submitInfo.fence = fence
        submitInfo.submit(queue)
        fence.waitForFence()
    }

    fun render() {
        ssaoSubmitInfo.submit(queue)
        ssaoBlurSubmitInfo.submit(queue)
    }

    init {
        val device = deviceBundle.logicalDevice.handle
        val memoryProperties = deviceBundle.physicalDevice.memoryProperties
        val descriptorPool = deviceBundle.logicalDevice.getDescriptorPool(Thread.currentThread().id)
        queue = deviceBundle.logicalDevice.computeQueue
        kernelSize = 64
        randomx = FloatArray(16)
        randomy = FloatArray(16)
        for (i in 0..15) {
            randomx[i] = Math.random().toFloat() * 2 - 1
            randomy[i] = Math.random().toFloat() * 2 - 1
        }
        kernel = generateRandomKernel4D(kernelSize)
        generateNoise(device, memoryProperties, descriptorPool,
                deviceBundle.logicalDevice.getComputeCommandPool(Thread.currentThread().id))
        ssaoImage = Image2DDeviceLocal(device, memoryProperties,
                width, height, VK10.VK_FORMAT_R16G16B16A16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT
                or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        ssaoImageView = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, ssaoImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        val sampler = VkSampler(device, VK10.VK_FILTER_LINEAR, false, 0f,
                VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)

        // ssao resources
        val pushConstantRange = java.lang.Float.BYTES * 21
        val pushConstants = MemoryUtil.memAlloc(pushConstantRange)
        pushConstants.put(createByteBuffer(camera.projectionMatrix))
        pushConstants.putFloat(1f)
        pushConstants.putFloat(0.02f)
        pushConstants.putFloat(kernelSize.toFloat())
        pushConstants.putFloat(width.toFloat())
        pushConstants.putFloat(height.toFloat())
        pushConstants.flip()
        kernelBuffer = createDeviceLocalBuffer(device, memoryProperties,
                deviceBundle.logicalDevice.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                deviceBundle.logicalDevice.transferQueue,
                createByteBuffer(kernel),
                VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT)
        ssaoDescriptorSetLayout = DescriptorSetLayout(device, 6)
        ssaoDescriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        ssaoDescriptorSetLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        ssaoDescriptorSetLayout.addLayoutBinding(2, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        ssaoDescriptorSetLayout.addLayoutBinding(3, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        ssaoDescriptorSetLayout.addLayoutBinding(4, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        ssaoDescriptorSetLayout.addLayoutBinding(5, VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        ssaoDescriptorSetLayout.create()
        ssaoDescriptorSet = DescriptorSet(device, descriptorPool!!.handle,
                ssaoDescriptorSetLayout.handlePointer)
        ssaoDescriptorSet.updateDescriptorImageBuffer(ssaoImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        ssaoDescriptorSet.updateDescriptorImageBuffer(worldPositionImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 1,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        ssaoDescriptorSet.updateDescriptorImageBuffer(normalImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 2,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        ssaoDescriptorSet.updateDescriptorImageBuffer(noiseImageView!!.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 3,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        ssaoDescriptorSet.updateDescriptorImageBuffer(depthImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, sampler.handle, 4,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        ssaoDescriptorSet.updateDescriptorBuffer(kernelBuffer.handle,
                java.lang.Float.BYTES * 3 * kernelSize.toLong(), 0, 5,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
        var descriptorSets: MutableList<DescriptorSet> = ArrayList()
        var descriptorSetLayouts: MutableList<DescriptorSetLayout> = ArrayList()
        descriptorSets.add(getCamera().descriptorSet)
        descriptorSets.add(ssaoDescriptorSet)
        descriptorSetLayouts.add(getCamera().descriptorSetLayout)
        descriptorSetLayouts.add(ssaoDescriptorSetLayout)
        ssaoPipeline = VkPipeline(device)
        ssaoPipeline.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, pushConstantRange)
        ssaoPipeline.setLayout(createLongBuffer(descriptorSetLayouts))
        ssaoPipeline.createComputePipeline(ComputeShader(device, "shaders/filter/ssao/ssao.comp.spv"))
        ssaoCmdBuffer = ComputeCmdBuffer(device,
                deviceBundle.logicalDevice.getComputeCommandPool(Thread.currentThread().id)!!.handle,
                ssaoPipeline.handle, ssaoPipeline.layoutHandle,
                createLongArray(descriptorSets), width / 16, height / 16, 1,
                pushConstants, VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        fence = Fence(device)
        ssaoSubmitInfo = SubmitInfo()
        ssaoSubmitInfo.setCommandBuffers(ssaoCmdBuffer.handlePointer)
        ssaoSubmitInfo.fence = fence

        // ssao blur resources
        ssaoBlurSceneImage = Image2DDeviceLocal(device, memoryProperties,
                width, height, VK10.VK_FORMAT_R16G16B16A16_SFLOAT, VK10.VK_IMAGE_USAGE_STORAGE_BIT
                or VK10.VK_IMAGE_USAGE_SAMPLED_BIT)
        ssaoBlurSceneImageView = VkImageView(device,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, ssaoBlurSceneImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        ssaoBlurDescriptorSetLayout = DescriptorSetLayout(device, 2)
        ssaoBlurDescriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        ssaoBlurDescriptorSetLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        ssaoBlurDescriptorSetLayout.create()
        ssaoBlurDescriptorSet = DescriptorSet(device, descriptorPool.handle,
                ssaoBlurDescriptorSetLayout.handlePointer)
        ssaoBlurDescriptorSet.updateDescriptorImageBuffer(ssaoBlurSceneImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 0,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        ssaoBlurDescriptorSet.updateDescriptorImageBuffer(ssaoImageView.handle,
                VK10.VK_IMAGE_LAYOUT_GENERAL, -1, 1,
                VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        descriptorSets = ArrayList()
        descriptorSetLayouts = ArrayList()
        descriptorSets.add(ssaoBlurDescriptorSet)
        descriptorSetLayouts.add(ssaoBlurDescriptorSetLayout)
        ssaoBlurPipeline = VkPipeline(device)
        ssaoBlurPipeline.setLayout(createLongBuffer(descriptorSetLayouts))
        ssaoBlurPipeline.createComputePipeline(ComputeShader(device, "shaders/filter/ssao/ssaoBlur.comp.spv"))
        ssaoBlurCmdBuffer = ComputeCmdBuffer(device,
                deviceBundle.logicalDevice.getComputeCommandPool(Thread.currentThread().id)!!.handle,
                ssaoBlurPipeline.handle, ssaoBlurPipeline.layoutHandle,
                createLongArray(descriptorSets), width / 16, height / 16, 1)
        fence = Fence(device)
        ssaoBlurSubmitInfo = SubmitInfo()
        ssaoBlurSubmitInfo.setCommandBuffers(ssaoBlurCmdBuffer.handlePointer)
        ssaoBlurSubmitInfo.fence = fence
    }
}