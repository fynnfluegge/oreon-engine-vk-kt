package org.oreon.vk.components.fft

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkQueue
import org.oreon.core.math.Vec2f
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
import org.oreon.core.vk.util.VkUtil.createLongArray
import org.oreon.core.vk.wrapper.image.Image2DDeviceLocal
import java.nio.ByteBuffer

class FFT(deviceBundle: VkDeviceBundle, N: Int, L: Int, t_delta: Float,
          amplitude: Float, direction: Vec2f?, intensity: Float, capillarSupressFactor: Float) {
    private val computeQueue: VkQueue?
    var dxImageView: VkImageView
    var dyImageView: VkImageView
    var dzImageView: VkImageView
    private val dxImage: VkImage
    private val dyImage: VkImage
    private val dzImage: VkImage
    private val twiddleFactors: TwiddleFactors
    private val h0k: H0k
    private val hkt: Hkt
    private val descriptorLayout: DescriptorSetLayout
    private val butterflyPipeline: VkPipeline
    private val inversionPipeline: VkPipeline
    private val butterflyShader: ShaderModule
    private val inversionShader: ShaderModule

    // dy fft resources
    private val dyButterflyDescriptorSet: DescriptorSet
    private val dyInversionDescriptorSet: DescriptorSet
    private val dyPingpongImage: VkImage
    private val dyPingpongImageView: VkImageView

    // dx fft resources
    private val dxButterflyDescriptorSet: DescriptorSet
    private val dxInversionDescriptorSet: DescriptorSet
    private val dxPingpongImage: VkImage
    private val dxPingpongImageView: VkImageView

    // dz fft resources
    private val dzButterflyDescriptorSet: DescriptorSet
    private val dzInversionDescriptorSet: DescriptorSet
    private val dzPingpongImage: VkImage
    private val dzPingpongImageView: VkImageView
    private val horizontalPushConstants: Array<ByteBuffer?>
    private val verticalPushConstants: Array<ByteBuffer?>
    private val inversionPushConstants: ByteBuffer
    private var fftCommandBuffer: CommandBuffer? = null
    private val fftSubmitInfo: SubmitInfo
    var fftSignalSemaphore: VkSemaphore
    fun record(deviceBundle: VkDeviceBundle, N: Int, stages: Int) {
        fftCommandBuffer = CommandBuffer(deviceBundle.logicalDevice.handle,
                deviceBundle.logicalDevice.getComputeCommandPool(Thread.currentThread().id)!!.handle,
                VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY)
        fftCommandBuffer!!.beginRecord(VK10.VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT)
        fftCommandBuffer!!.bindComputePipelineCmd(butterflyPipeline.handle)

        // horizontal
        for (i in 0 until stages) {

            // dy
            fftCommandBuffer!!.pushConstantsCmd(butterflyPipeline.layoutHandle,
                    VK10.VK_SHADER_STAGE_COMPUTE_BIT, horizontalPushConstants[i])
            fftCommandBuffer!!.bindComputeDescriptorSetsCmd(butterflyPipeline.layoutHandle,
                    createLongArray(dyButterflyDescriptorSet))
            fftCommandBuffer!!.dispatchCmd(N / 16, N / 16, 1)

            // dx
            fftCommandBuffer!!.pushConstantsCmd(butterflyPipeline.layoutHandle,
                    VK10.VK_SHADER_STAGE_COMPUTE_BIT, horizontalPushConstants[i])
            fftCommandBuffer!!.bindComputeDescriptorSetsCmd(butterflyPipeline.layoutHandle,
                    createLongArray(dxButterflyDescriptorSet))
            fftCommandBuffer!!.dispatchCmd(N / 16, N / 16, 1)

            // dz
            fftCommandBuffer!!.pushConstantsCmd(butterflyPipeline.layoutHandle,
                    VK10.VK_SHADER_STAGE_COMPUTE_BIT, horizontalPushConstants[i])
            fftCommandBuffer!!.bindComputeDescriptorSetsCmd(butterflyPipeline.layoutHandle,
                    createLongArray(dzButterflyDescriptorSet))
            fftCommandBuffer!!.dispatchCmd(N / 16, N / 16, 1)
            fftCommandBuffer!!.pipelineMemoryBarrierCmd(
                    VK10.VK_ACCESS_SHADER_WRITE_BIT, VK10.VK_ACCESS_SHADER_READ_BIT,
                    VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                    VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT)
        }

        // vertical
        for (i in 0 until stages) {

            // dy
            fftCommandBuffer!!.pushConstantsCmd(butterflyPipeline.layoutHandle,
                    VK10.VK_SHADER_STAGE_COMPUTE_BIT, verticalPushConstants[i])
            fftCommandBuffer!!.bindComputeDescriptorSetsCmd(butterflyPipeline.layoutHandle,
                    createLongArray(dyButterflyDescriptorSet))
            fftCommandBuffer!!.dispatchCmd(N / 16, N / 16, 1)

            // dx
            fftCommandBuffer!!.pushConstantsCmd(butterflyPipeline.layoutHandle,
                    VK10.VK_SHADER_STAGE_COMPUTE_BIT, verticalPushConstants[i])
            fftCommandBuffer!!.bindComputeDescriptorSetsCmd(butterflyPipeline.layoutHandle,
                    createLongArray(dxButterflyDescriptorSet))
            fftCommandBuffer!!.dispatchCmd(N / 16, N / 16, 1)

            // dz
            fftCommandBuffer!!.pushConstantsCmd(butterflyPipeline.layoutHandle,
                    VK10.VK_SHADER_STAGE_COMPUTE_BIT, verticalPushConstants[i])
            fftCommandBuffer!!.bindComputeDescriptorSetsCmd(butterflyPipeline.layoutHandle,
                    createLongArray(dzButterflyDescriptorSet))
            fftCommandBuffer!!.dispatchCmd(N / 16, N / 16, 1)
            fftCommandBuffer!!.pipelineMemoryBarrierCmd(
                    VK10.VK_ACCESS_SHADER_WRITE_BIT, VK10.VK_ACCESS_SHADER_READ_BIT,
                    VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                    VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT)
        }

        // inversion
        fftCommandBuffer!!.bindComputePipelineCmd(inversionPipeline.handle)
        fftCommandBuffer!!.pushConstantsCmd(inversionPipeline.layoutHandle,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT, inversionPushConstants)
        // dy
        fftCommandBuffer!!.bindComputeDescriptorSetsCmd(inversionPipeline.layoutHandle,
                createLongArray(dyInversionDescriptorSet))
        fftCommandBuffer!!.dispatchCmd(N / 16, N / 16, 1)

        // dx
        fftCommandBuffer!!.bindComputeDescriptorSetsCmd(inversionPipeline.layoutHandle,
                createLongArray(dxInversionDescriptorSet))
        fftCommandBuffer!!.dispatchCmd(N / 16, N / 16, 1)

        // dz
        fftCommandBuffer!!.bindComputeDescriptorSetsCmd(inversionPipeline.layoutHandle,
                createLongArray(dzInversionDescriptorSet))
        fftCommandBuffer!!.dispatchCmd(N / 16, N / 16, 1)
        fftCommandBuffer!!.finishRecord()
    }

    fun render() {
        hkt.render()
        fftSubmitInfo.submit(computeQueue)
    }

    private inner class ButterflyDescriptorSet(device: VkDevice?, descriptorPool: DescriptorPool?,
                                               layout: DescriptorSetLayout, twiddleFactors: VkImageView,
                                               coefficients: VkImageView, pingpongImage: VkImageView) : DescriptorSet(device!!, descriptorPool!!.handle, layout.handlePointer) {
        init {
            updateDescriptorImageBuffer(twiddleFactors.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1,
                    0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
            updateDescriptorImageBuffer(coefficients.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1,
                    1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
            updateDescriptorImageBuffer(pingpongImage.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1,
                    2, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        }
    }

    private inner class InversionDescriptorSet(device: VkDevice?, descriptorPool: DescriptorPool?,
                                               layout: DescriptorSetLayout, spatialDomain: VkImageView,
                                               coefficients: VkImageView, pingpongImage: VkImageView) : DescriptorSet(device!!, descriptorPool!!.handle, layout.handlePointer) {
        init {
            updateDescriptorImageBuffer(spatialDomain.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1,
                    0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
            updateDescriptorImageBuffer(coefficients.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1,
                    1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
            updateDescriptorImageBuffer(pingpongImage.handle,
                    VK10.VK_IMAGE_LAYOUT_GENERAL, -1,
                    2, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
        }
    }

    fun destroy() {
        twiddleFactors.destroy()
        h0k.destroy()
        hkt.destroy()
        dxImageView.destroy()
        dyImageView.destroy()
        dzImageView.destroy()
        dxImage.destroy()
        dyImage.destroy()
        dzImage.destroy()
        descriptorLayout.destroy()
        butterflyPipeline.destroy()
        inversionPipeline.destroy()
        dyButterflyDescriptorSet.destroy()
        dyInversionDescriptorSet.destroy()
        dyPingpongImageView.destroy()
        dyPingpongImage.destroy()
        dxButterflyDescriptorSet.destroy()
        dxInversionDescriptorSet.destroy()
        dxPingpongImageView.destroy()
        dxPingpongImage.destroy()
        dzButterflyDescriptorSet.destroy()
        dzInversionDescriptorSet.destroy()
        dzPingpongImageView.destroy()
        dzPingpongImage.destroy()
        fftCommandBuffer!!.destroy()
        fftSignalSemaphore.destroy()
    }

    init {
        val device = deviceBundle.logicalDevice.handle
        val memoryProperties = deviceBundle.physicalDevice.memoryProperties
        val descriptorPool = deviceBundle.logicalDevice.getDescriptorPool(Thread.currentThread().id)
        computeQueue = deviceBundle.logicalDevice.computeQueue
        val stages = (Math.log(N.toDouble()) / Math.log(2.0)).toInt()
        dyPingpongImage = Image2DDeviceLocal(device, memoryProperties, N, N,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT,
                VK10.VK_IMAGE_USAGE_SAMPLED_BIT or VK10.VK_IMAGE_USAGE_STORAGE_BIT)
        dyPingpongImageView = VkImageView(device,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT, dyPingpongImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        dxPingpongImage = Image2DDeviceLocal(device, memoryProperties, N, N,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT,
                VK10.VK_IMAGE_USAGE_SAMPLED_BIT or VK10.VK_IMAGE_USAGE_STORAGE_BIT)
        dxPingpongImageView = VkImageView(device,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT, dxPingpongImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        dzPingpongImage = Image2DDeviceLocal(device, memoryProperties, N, N,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT,
                VK10.VK_IMAGE_USAGE_SAMPLED_BIT or VK10.VK_IMAGE_USAGE_STORAGE_BIT)
        dzPingpongImageView = VkImageView(device,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT, dzPingpongImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        dyImage = Image2DDeviceLocal(device, memoryProperties, N, N,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT,
                VK10.VK_IMAGE_USAGE_SAMPLED_BIT or VK10.VK_IMAGE_USAGE_STORAGE_BIT)
        dyImageView = VkImageView(device,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT, dyImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        dxImage = Image2DDeviceLocal(device, memoryProperties, N, N,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT,
                VK10.VK_IMAGE_USAGE_SAMPLED_BIT or VK10.VK_IMAGE_USAGE_STORAGE_BIT)
        dxImageView = VkImageView(device,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT, dxImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        dzImage = Image2DDeviceLocal(device, memoryProperties, N, N,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT,
                VK10.VK_IMAGE_USAGE_SAMPLED_BIT or VK10.VK_IMAGE_USAGE_STORAGE_BIT)
        dzImageView = VkImageView(device,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT, dzImage.handle, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
        twiddleFactors = TwiddleFactors(deviceBundle, N)
        h0k = H0k(deviceBundle, N, L, amplitude, direction!!, intensity, capillarSupressFactor)
        hkt = Hkt(deviceBundle, N, L, t_delta, h0k.h0k_imageView, h0k.h0minusk_imageView)
        horizontalPushConstants = arrayOfNulls(stages)
        verticalPushConstants = arrayOfNulls(stages)
        var pingpong = 0
        for (i in 0 until stages) {
            horizontalPushConstants[i] = MemoryUtil.memAlloc(Integer.BYTES * 4)
            horizontalPushConstants[i]?.putInt(i)
            horizontalPushConstants[i]?.putInt(pingpong)
            horizontalPushConstants[i]?.putInt(0)
            horizontalPushConstants[i]?.flip()
            pingpong++
            pingpong %= 2
        }
        for (i in 0 until stages) {
            verticalPushConstants[i] = MemoryUtil.memAlloc(Integer.BYTES * 4)
            verticalPushConstants[i]?.putInt(i)
            verticalPushConstants[i]?.putInt(pingpong)
            verticalPushConstants[i]?.putInt(1)
            verticalPushConstants[i]?.flip()
            pingpong++
            pingpong %= 2
        }
        inversionPushConstants = MemoryUtil.memAlloc(Integer.BYTES * 2)
        inversionPushConstants.putInt(N)
        inversionPushConstants.putInt(pingpong)
        inversionPushConstants.flip()
        val pushConstants = MemoryUtil.memAlloc(Integer.BYTES * 1)
        val intBuffer = pushConstants.asIntBuffer()
        intBuffer.put(N)
        descriptorLayout = DescriptorSetLayout(device, 4)
        descriptorLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorLayout.addLayoutBinding(2, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorLayout.addLayoutBinding(3, VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        descriptorLayout.create()
        dyButterflyDescriptorSet = ButterflyDescriptorSet(device, descriptorPool,
                descriptorLayout, twiddleFactors.imageView,
                hkt.dyCoefficients_imageView, dyPingpongImageView)
        dyInversionDescriptorSet = InversionDescriptorSet(device, descriptorPool,
                descriptorLayout, dyImageView, hkt.dyCoefficients_imageView,
                dyPingpongImageView)
        dxButterflyDescriptorSet = ButterflyDescriptorSet(device, descriptorPool,
                descriptorLayout, twiddleFactors.imageView,
                hkt.dxCoefficients_imageView,
                dxPingpongImageView)
        dxInversionDescriptorSet = InversionDescriptorSet(device, descriptorPool,
                descriptorLayout, dxImageView, hkt.dxCoefficients_imageView,
                dxPingpongImageView)
        dzButterflyDescriptorSet = ButterflyDescriptorSet(device, descriptorPool,
                descriptorLayout, twiddleFactors.imageView,
                hkt.dzCoefficients_imageView,
                dzPingpongImageView)
        dzInversionDescriptorSet = InversionDescriptorSet(device, descriptorPool,
                descriptorLayout, dzImageView, hkt.dzCoefficients_imageView,
                dzPingpongImageView)
        butterflyShader = ShaderModule(device, "shaders/fft/Butterfly.comp.spv", VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        inversionShader = ShaderModule(device, "shaders/fft/Inversion.comp.spv", VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        butterflyPipeline = VkPipeline(device)
        butterflyPipeline.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, Integer.BYTES * 4)
        butterflyPipeline.setLayout(descriptorLayout.handlePointer)
        butterflyPipeline.createComputePipeline(butterflyShader)
        inversionPipeline = VkPipeline(device)
        inversionPipeline.setPushConstantsRange(VK10.VK_SHADER_STAGE_COMPUTE_BIT, Integer.BYTES * 2)
        inversionPipeline.setLayout(descriptorLayout.handlePointer)
        inversionPipeline.createComputePipeline(inversionShader)
        butterflyShader.destroy()
        inversionShader.destroy()
        record(deviceBundle, N, stages)
        fftSignalSemaphore = VkSemaphore(device)
        val pWaitDstStageMask = MemoryUtil.memAllocInt(1)
        pWaitDstStageMask.put(0, VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT)
        fftSubmitInfo = SubmitInfo()
        fftSubmitInfo.setCommandBuffers(fftCommandBuffer!!.handlePointer)
        fftSubmitInfo.setWaitSemaphores(hkt.signalSemaphore.handlePointer)
        fftSubmitInfo.setWaitDstStageMask(pWaitDstStageMask)
        fftSubmitInfo.setSignalSemaphores(fftSignalSemaphore.handlePointer)
    }
}