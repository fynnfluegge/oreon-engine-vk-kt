package org.oreon.core.vk.swapchain

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.oreon.core.context.BaseContext.Companion.window
import org.oreon.core.model.Vertex.VertexLayout
import org.oreon.core.util.BufferUtil
import org.oreon.core.util.MeshGenerator
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.command.SubmitInfo
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import org.oreon.core.vk.device.LogicalDevice
import org.oreon.core.vk.device.PhysicalDevice
import org.oreon.core.vk.framebuffer.VkFrameBuffer
import org.oreon.core.vk.image.VkImageView
import org.oreon.core.vk.image.VkSampler
import org.oreon.core.vk.memory.VkBuffer
import org.oreon.core.vk.pipeline.RenderPass
import org.oreon.core.vk.pipeline.ShaderPipeline
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.pipeline.VkVertexInput
import org.oreon.core.vk.synchronization.Fence
import org.oreon.core.vk.synchronization.VkSemaphore
import org.oreon.core.vk.util.VkUtil
import org.oreon.core.vk.wrapper.buffer.VkBufferHelper
import org.oreon.core.vk.wrapper.command.DrawCmdBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer
import java.util.*

class SwapChain(logicalDevice: LogicalDevice,
                physicalDevice: PhysicalDevice,
                surface: Long,
                imageView: Long) {

    private val handle: Long
    val drawFence: Fence
    private val pHandle: LongBuffer
    private val extent: VkExtent2D
    private lateinit var swapChainImages: MutableList<Long>
    private lateinit var swapChainImageViews: MutableList<VkImageView>
    private lateinit var frameBuffers: MutableList<VkFrameBuffer>
    private lateinit var renderCommandBuffers: MutableList<CommandBuffer>
    private val presentInfo: VkPresentInfoKHR
    private val pAcquiredImageIndex: IntBuffer
    private val renderCompleteSemaphore: VkSemaphore
    private val imageAcquiredSemaphore: VkSemaphore
    private val submitInfo: SubmitInfo
    private val vertexBufferObject: VkBuffer
    private val indexBufferObject: VkBuffer
    private var pipeline: VkPipeline? = null
    private var renderPass: RenderPass? = null
    private var sampler: VkSampler? = null
    private var descriptorSet: DescriptorSet? = null
    private var descriptorSetLayout: DescriptorSetLayout? = null
    private val device: VkDevice
    private val UINT64_MAX = -0x1L

    fun createDescriptor(descriptorPool: Long, imageView: Long) {
        descriptorSetLayout = DescriptorSetLayout(device, 1)
        descriptorSetLayout!!.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        descriptorSetLayout!!.create()
        sampler = VkSampler(device, VK10.VK_FILTER_NEAREST, false, 0f,
                VK10.VK_SAMPLER_MIPMAP_MODE_NEAREST, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)
        descriptorSet = DescriptorSet(device, descriptorPool, descriptorSetLayout!!.handlePointer)
        descriptorSet!!.updateDescriptorImageBuffer(imageView, VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                sampler!!.handle, 0, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
    }

    fun createRenderPass(imageFormat: Int) {
        renderPass = RenderPass(device)
        renderPass!!.addColorAttachment(0, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, imageFormat, 1,
                VK10.VK_IMAGE_LAYOUT_UNDEFINED, KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
        renderPass!!.addSubpassDependency(VK10.VK_SUBPASS_EXTERNAL, 0, VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, 0,
                VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT or VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT, 0)
        renderPass!!.createSubpass()
        renderPass!!.createRenderPass()
    }

    fun createPipeline(renderPass: Long, layouts: LongBuffer?) {
        val shaderPipeline = ShaderPipeline(device)
        shaderPipeline.createVertexShader("shaders/quad/quad.vert.spv")
        shaderPipeline.createFragmentShader("shaders/quad/quad.frag.spv")
        shaderPipeline.createShaderPipeline()
        val vertexInputInfo = VkVertexInput(VertexLayout.POS_UV)
        pipeline = VkPipeline(device)
        pipeline!!.setVertexInput(vertexInputInfo)
        pipeline!!.setInputAssembly(VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
        pipeline!!.setViewportAndScissor(extent.width(), extent.height())
        pipeline!!.setRasterizer()
        pipeline!!.setMultisamplingState(1)
        pipeline!!.addColorBlendAttachment()
        pipeline!!.setColorBlendState()
        pipeline!!.setDepthAndStencilTest(false)
        pipeline!!.setDynamicState()
        pipeline!!.setLayout(layouts)
        pipeline!!.createGraphicsPipeline(shaderPipeline, renderPass)
        shaderPipeline.destroy()
    }

    fun createImages() {
        val pImageCount = MemoryUtil.memAllocInt(1)
        var err = KHRSwapchain.vkGetSwapchainImagesKHR(device, handle, pImageCount, null)
        val imageCount = pImageCount[0]
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to get number of swapchain images: " + VkUtil.translateVulkanResult(err))
        }
        val pSwapchainImages = MemoryUtil.memAllocLong(imageCount)
        err = KHRSwapchain.vkGetSwapchainImagesKHR(device, handle, pImageCount, pSwapchainImages)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to get swapchain images: " + VkUtil.translateVulkanResult(err))
        }
        swapChainImages = ArrayList<Long>(imageCount)
        for (i in 0 until imageCount) {
            swapChainImages.add(pSwapchainImages[i])
        }
        MemoryUtil.memFree(pImageCount)
        MemoryUtil.memFree(pSwapchainImages)
    }

    fun createImageViews(imageFormat: Int) {
        swapChainImageViews = ArrayList<VkImageView>(swapChainImages!!.size)
        for (swapChainImage in swapChainImages!!) {
            val imageView = VkImageView(device, imageFormat, swapChainImage,
                    VK10.VK_IMAGE_ASPECT_COLOR_BIT)
            swapChainImageViews.add(imageView)
        }
    }

    fun createFrameBuffers(renderPass: Long) {
        frameBuffers = ArrayList<VkFrameBuffer>(swapChainImages!!.size)
        for (imageView in swapChainImageViews!!) {
            val pAttachments = MemoryUtil.memAllocLong(1)
            pAttachments.put(0, imageView.handle)
            val frameBuffer = VkFrameBuffer(device,
                    extent.width(), extent.height(), 1, pAttachments, renderPass)
            frameBuffers.add(frameBuffer)
        }
    }

    fun createRenderCommandBuffers(commandPool: Long, renderPass: Long,
                                   vertexBuffer: Long, indexBuffer: Long, indexCount: Int,
                                   descriptorSets: LongArray?) {
        renderCommandBuffers = ArrayList<CommandBuffer>()
        for (frameBuffer in frameBuffers!!) {
            val commandBuffer: CommandBuffer = DrawCmdBuffer(
                    device, commandPool, pipeline!!.handle,
                    pipeline!!.layoutHandle, renderPass,
                    frameBuffer.handle, extent.width(), extent.height(),
                    1, 0, descriptorSets, vertexBuffer, indexBuffer, indexCount)
            renderCommandBuffers.add(commandBuffer)
        }
    }

    fun draw(queue: VkQueue?, waitSemaphore: VkSemaphore?) {
        val err = KHRSwapchain.vkAcquireNextImageKHR(device, handle, UINT64_MAX, imageAcquiredSemaphore.handle, VK10.VK_NULL_HANDLE, pAcquiredImageIndex)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to acquire next swapchain image: " + VkUtil.translateVulkanResult(err))
        }
        val currentRenderCommandBuffer = renderCommandBuffers!![pAcquiredImageIndex[0]]
        val pWaitDstStageMask = MemoryUtil.memAllocInt(2)
        pWaitDstStageMask.put(0, VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
        pWaitDstStageMask.put(1, VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
        val pWaitSemaphores = MemoryUtil.memAllocLong(2)
        pWaitSemaphores.put(0, imageAcquiredSemaphore.handle)
        pWaitSemaphores.put(1, waitSemaphore!!.handle)
        submitInfo.setCommandBuffers(currentRenderCommandBuffer.handlePointer)
        submitInfo.setWaitDstStageMask(pWaitDstStageMask)
        submitInfo.setWaitSemaphores(pWaitSemaphores)
        submitInfo.submit(queue)
        VkUtil.vkCheckResult(KHRSwapchain.vkQueuePresentKHR(queue, presentInfo))
    }

    fun destroy() {
        for (imageView in swapChainImageViews!!) {
            imageView.destroy()
        }
        for (framebuffer in frameBuffers!!) {
            framebuffer.destroy()
        }
        for (commandbuffer in renderCommandBuffers!!) {
            commandbuffer.destroy()
        }
        vertexBufferObject.destroy()
        indexBufferObject.destroy()
        renderCompleteSemaphore.destroy()
        imageAcquiredSemaphore.destroy()
        descriptorSet!!.destroy()
        descriptorSetLayout!!.destroy()
        sampler!!.destroy()
        pipeline!!.destroy()
        renderPass!!.destroy()
        drawFence.destroy()
        KHRSwapchain.vkDestroySwapchainKHR(device, handle, null)
    }

    init {
        device = logicalDevice.handle
        extent = physicalDevice.swapChainCapabilities.surfaceCapabilities.currentExtent()
        extent.width(window.width)
        extent.height(window.height)
        val imageFormat = VK10.VK_FORMAT_B8G8R8A8_UNORM
        val colorSpace = KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
        physicalDevice.checkDeviceFormatAndColorSpaceSupport(imageFormat, colorSpace)
        var presentMode = KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR
        if (!physicalDevice.checkDevicePresentationModeSupport(presentMode)) {
            presentMode = if (physicalDevice.checkDevicePresentationModeSupport(KHRSurface.VK_PRESENT_MODE_FIFO_KHR)) KHRSurface.VK_PRESENT_MODE_FIFO_KHR else KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR
        }
        val minImageCount = physicalDevice.deviceMinImageCount4TripleBuffering
        createDescriptor(logicalDevice.getDescriptorPool(Thread.currentThread().id)!!.handle,
                imageView)
        createRenderPass(imageFormat)
        createPipeline(renderPass!!.handle,
                descriptorSetLayout!!.handlePointer)
        val swapchainCreateInfo = VkSwapchainCreateInfoKHR.calloc()
                .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .pNext(0)
                .surface(surface)
                .oldSwapchain(VK10.VK_NULL_HANDLE)
                .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                .imageUsage(VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                .preTransform(KHRSurface.VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR)
                .minImageCount(minImageCount)
                .imageFormat(imageFormat)
                .imageColorSpace(colorSpace)
                .imageExtent(extent)
                .presentMode(presentMode)
                .imageArrayLayers(1)
                .clipped(true) // presentation queue family and graphics queue family are the same
                .imageSharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE)
                .pQueueFamilyIndices(null)
        pHandle = MemoryUtil.memAllocLong(1)
        val err = KHRSwapchain.vkCreateSwapchainKHR(device, swapchainCreateInfo, null, pHandle)
        handle = pHandle[0]
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create swap chain: " + VkUtil.translateVulkanResult(err))
        }
        createImages()
        createImageViews(imageFormat)
        createFrameBuffers(renderPass!!.handle)
        renderCompleteSemaphore = VkSemaphore(device)
        imageAcquiredSemaphore = VkSemaphore(device)
        pAcquiredImageIndex = MemoryUtil.memAllocInt(1)
        presentInfo = VkPresentInfoKHR.calloc()
                .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pNext(0)
                .pWaitSemaphores(renderCompleteSemaphore.handlePointer)
                .swapchainCount(1)
                .pSwapchains(pHandle)
                .pImageIndices(pAcquiredImageIndex)
                .pResults(null)
        swapchainCreateInfo.free()
        val fullScreenQuad = MeshGenerator.NDCQuad2D()
        val vertexBuffer = BufferUtil.createByteBuffer(fullScreenQuad.vertices, VertexLayout.POS_UV)
        val indexBuffer = BufferUtil.createByteBuffer(*fullScreenQuad.indices)
        vertexBufferObject = VkBufferHelper.createDeviceLocalBuffer(device,
                physicalDevice.memoryProperties,
                logicalDevice.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                logicalDevice.transferQueue,
                vertexBuffer, VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
        indexBufferObject = VkBufferHelper.createDeviceLocalBuffer(device,
                physicalDevice.memoryProperties,
                logicalDevice.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                logicalDevice.transferQueue,
                indexBuffer, VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT)
        createRenderCommandBuffers(logicalDevice.getGraphicsCommandPool(Thread.currentThread().id)!!.handle,
                renderPass!!.handle,
                vertexBufferObject.handle,
                indexBufferObject.handle,
                fullScreenQuad.indices.size,
                descriptorSet?.let { VkUtil.createLongArray(it) })
        drawFence = Fence(device)
        submitInfo = SubmitInfo()
        submitInfo.setSignalSemaphores(renderCompleteSemaphore.handlePointer)
        submitInfo.fence = drawFence
    }
}