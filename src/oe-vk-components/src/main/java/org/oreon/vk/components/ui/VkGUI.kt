package org.oreon.vk.components.ui

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VkQueue
import org.oreon.common.ui.GUI
import org.oreon.common.ui.UIPanelLoader.load
import org.oreon.core.context.BaseContext.Companion.config
import org.oreon.core.model.Vertex.VertexLayout
import org.oreon.core.scenegraph.NodeComponentType
import org.oreon.core.scenegraph.RenderList
import org.oreon.core.target.FrameBufferObject
import org.oreon.core.util.BufferUtil
import org.oreon.core.util.MeshGenerator
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.command.SubmitInfo
import org.oreon.core.vk.context.DeviceManager.DeviceType
import org.oreon.core.vk.context.VkContext.deviceManager
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import org.oreon.core.vk.framebuffer.FrameBufferColorAttachment
import org.oreon.core.vk.framebuffer.VkFrameBuffer
import org.oreon.core.vk.framebuffer.VkFrameBufferObject
import org.oreon.core.vk.image.VkImageView
import org.oreon.core.vk.image.VkSampler
import org.oreon.core.vk.pipeline.RenderPass
import org.oreon.core.vk.pipeline.ShaderPipeline
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.pipeline.VkVertexInput
import org.oreon.core.vk.scenegraph.VkMeshData
import org.oreon.core.vk.scenegraph.VkRenderInfo
import org.oreon.core.vk.synchronization.VkSemaphore
import org.oreon.core.vk.util.VkUtil
import org.oreon.core.vk.wrapper.buffer.VkBufferHelper
import org.oreon.core.vk.wrapper.command.PrimaryCmdBuffer
import org.oreon.core.vk.wrapper.command.SecondaryDrawIndexedCmdBuffer
import org.oreon.core.vk.wrapper.image.VkImageBundle
import org.oreon.core.vk.wrapper.image.VkImageHelper
import org.oreon.core.vk.wrapper.pipeline.GraphicsPipeline
import java.nio.LongBuffer
import java.util.*

open class VkGUI : GUI() {
    protected lateinit var guiOverlayFbo: VkFrameBufferObject
    protected lateinit var fontsImageBundle: VkImageBundle
    protected lateinit var panelMeshBuffer: VkMeshData
    private lateinit var guiPrimaryCmdBuffer: PrimaryCmdBuffer
    private lateinit var guiSecondaryCmdBuffers: LinkedHashMap<String, CommandBuffer?>
    private lateinit var guiRenderList: RenderList
    private lateinit var guiSubmitInfo: SubmitInfo
    private lateinit var queue: VkQueue

    // underlay image resources
    private lateinit var underlayImageCmdBuffer: CommandBuffer
    private lateinit var underlayImagePipeline: VkPipeline
    private lateinit var underlayImageDescriptorSet: DescriptorSet
    private lateinit var underlayImageDescriptorSetLayout: DescriptorSetLayout
    private lateinit var underlayImageSampler: VkSampler

    lateinit var signalSemaphore: VkSemaphore

    open fun init(underlayImageView: VkImageView, waitSemaphores: LongBuffer?) {
        val device = deviceManager
                .getLogicalDevice(DeviceType.MAJOR_GRAPHICS_DEVICE)
        val memoryProperties = deviceManager
                .getPhysicalDevice(DeviceType.MAJOR_GRAPHICS_DEVICE).memoryProperties
        queue = device.graphicsQueue
        guiOverlayFbo = SingleAttachmentFbo(device.handle, memoryProperties)
        guiRenderList = RenderList()
        guiSecondaryCmdBuffers = LinkedHashMap()
        guiPrimaryCmdBuffer = PrimaryCmdBuffer(device.handle,
                device.getGraphicsCommandPool(Thread.currentThread().id)!!.handle)
        val pWaitDstStageMask = MemoryUtil.memAllocInt(1)
        pWaitDstStageMask.put(0, VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
        signalSemaphore = VkSemaphore(device.handle)
        guiSubmitInfo = SubmitInfo()
        guiSubmitInfo!!.setCommandBuffers(guiPrimaryCmdBuffer!!.handlePointer)
        guiSubmitInfo!!.setWaitSemaphores(waitSemaphores!!)
        guiSubmitInfo!!.setWaitDstStageMask(pWaitDstStageMask)
        guiSubmitInfo!!.setSignalSemaphores(signalSemaphore!!.handlePointer)

        // fonts Image 
        val fontsImage = VkImageHelper.loadImageFromFile(
                device.handle, memoryProperties,
                device.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                device.transferQueue,
                "gui/tex/Fonts.png",
                VK10.VK_IMAGE_USAGE_SAMPLED_BIT,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                VK10.VK_QUEUE_FAMILY_IGNORED)
        val fontsImageView = VkImageView(device.handle,
                VK10.VK_FORMAT_R8G8B8A8_UNORM, fontsImage.handle,
                VK10.VK_IMAGE_ASPECT_COLOR_BIT, 1)
        val sampler = VkSampler(device.handle, VK10.VK_FILTER_LINEAR, false, 1f,
                VK10.VK_SAMPLER_MIPMAP_MODE_NEAREST, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)
        fontsImageBundle = VkImageBundle(fontsImage, fontsImageView, sampler)

        // panel mesh buffer
        panelMeshBuffer = VkMeshData(device.handle,
                memoryProperties, device.getTransferCommandPool(Thread.currentThread().id),
                device.transferQueue, load("gui/basicPanel.gui"),
                VertexLayout.POS2D)
        panelMeshBuffer.create()

        // fullscreen underlay Image resources
        val shaderPipeline = ShaderPipeline(device.handle)
        shaderPipeline.createVertexShader("shaders/quad/quad.vert.spv")
        shaderPipeline.createFragmentShader("shaders/quad/quad.frag.spv")
        shaderPipeline.createShaderPipeline()
        val vertexInputInfo = VkVertexInput(VertexLayout.POS_UV)
        val fullScreenQuad = MeshGenerator.NDCQuad2D()
        val vertexBuffer = BufferUtil.createByteBuffer(fullScreenQuad.vertices, VertexLayout.POS_UV)
        val indexBuffer = BufferUtil.createByteBuffer(*fullScreenQuad.indices)
        val vertexBufferObject = VkBufferHelper.createDeviceLocalBuffer(
                device.handle, memoryProperties,
                device.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                device.transferQueue,
                vertexBuffer, VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
        val indexBufferObject = VkBufferHelper.createDeviceLocalBuffer(
                device.handle, memoryProperties,
                device.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                device.transferQueue,
                indexBuffer, VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT)
        underlayImageDescriptorSetLayout = DescriptorSetLayout(device.handle, 1)
        underlayImageDescriptorSetLayout!!.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        underlayImageDescriptorSetLayout!!.create()
        underlayImageSampler = VkSampler(device.handle, VK10.VK_FILTER_NEAREST, false, 0f,
                VK10.VK_SAMPLER_MIPMAP_MODE_NEAREST, 0f, VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)
        underlayImageDescriptorSet = DescriptorSet(device.handle,
                device.getDescriptorPool(Thread.currentThread().id)!!.handle,
                underlayImageDescriptorSetLayout!!.handlePointer)
        underlayImageDescriptorSet!!.updateDescriptorImageBuffer(underlayImageView.handle,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                underlayImageSampler!!.handle, 0, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        val descriptorSets: MutableList<DescriptorSet> = ArrayList()
        val descriptorSetLayouts: MutableList<DescriptorSetLayout> = ArrayList()
        descriptorSets.add(underlayImageDescriptorSet!!)
        descriptorSetLayouts.add(underlayImageDescriptorSetLayout!!)
        underlayImagePipeline = GraphicsPipeline(device.handle,
                shaderPipeline, vertexInputInfo, VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
                VkUtil.createLongBuffer(descriptorSetLayouts),
                guiOverlayFbo.width, guiOverlayFbo.height,
                guiOverlayFbo.renderPass!!.handle,
                guiOverlayFbo.colorAttachmentCount, 1)
        underlayImageCmdBuffer = SecondaryDrawIndexedCmdBuffer(
                device.handle,
                device.getGraphicsCommandPool(Thread.currentThread().id)!!.handle,
                underlayImagePipeline.handle, underlayImagePipeline.layoutHandle,
                guiOverlayFbo.frameBuffer!!.handle,
                guiOverlayFbo.renderPass!!.handle,
                0,
                VkUtil.createLongArray(descriptorSets),
                vertexBufferObject.handle, indexBufferObject.handle,
                fullScreenQuad.indices.size)
        guiSecondaryCmdBuffers!!["0"] = underlayImageCmdBuffer
        shaderPipeline.destroy()
    }

    override fun render() {
        record(guiRenderList)
        for (key in guiRenderList!!.keySet) {
            if (!guiSecondaryCmdBuffers!!.containsKey(key)) {
                val mainRenderInfo: VkRenderInfo? = guiRenderList!![key]?.getComponent(NodeComponentType.MAIN_RENDERINFO)
                guiSecondaryCmdBuffers!![key] = mainRenderInfo!!.commandBuffer
            }
        }

        // primary render command buffer
        if (!guiRenderList!!.objectList.isEmpty()) {
            guiPrimaryCmdBuffer!!.reset()
            guiPrimaryCmdBuffer!!.record(guiOverlayFbo!!.renderPass!!.handle,
                    guiOverlayFbo!!.frameBuffer!!.handle,
                    guiOverlayFbo!!.width,
                    guiOverlayFbo!!.height,
                    guiOverlayFbo!!.colorAttachmentCount,
                    guiOverlayFbo!!.depthAttachmentCount,
                    VkUtil.createPointerBuffer(guiSecondaryCmdBuffers!!.values))
            guiSubmitInfo!!.submit(queue)
        }
    }

    private inner class SingleAttachmentFbo(device: VkDevice?,
                                            memoryProperties: VkPhysicalDeviceMemoryProperties?) : VkFrameBufferObject() {
        init {
            width = config.frameWidth
            height = config.frameHeight
            val colorAttachment: VkImageBundle = FrameBufferColorAttachment(device!!, memoryProperties!!,
                    width, height, VK10.VK_FORMAT_R16G16B16A16_SFLOAT, 1)
            attachments[Attachment.COLOR] = colorAttachment
            renderPass = RenderPass(device)
            renderPass!!.addColorAttachment(0, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                    VK10.VK_FORMAT_R16G16B16A16_SFLOAT, 1, VK10.VK_IMAGE_LAYOUT_UNDEFINED,
                    VK10.VK_IMAGE_LAYOUT_GENERAL)
            renderPass!!.addSubpassDependency(VK10.VK_SUBPASS_EXTERNAL, 0,
                    VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                    VK10.VK_ACCESS_MEMORY_READ_BIT,
                    VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT or VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT,
                    VK10.VK_DEPENDENCY_BY_REGION_BIT)
            renderPass!!.addSubpassDependency(0, VK10.VK_SUBPASS_EXTERNAL,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                    VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                    VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT or VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT,
                    VK10.VK_ACCESS_MEMORY_READ_BIT,
                    VK10.VK_DEPENDENCY_BY_REGION_BIT)
            renderPass!!.createSubpass()
            renderPass!!.createRenderPass()
            depthAttachmentCount = 0
            colorAttachmentCount = renderPass!!.attachmentCount - depthAttachmentCount
            val pImageViews = MemoryUtil.memAllocLong(renderPass!!.attachmentCount)
            pImageViews.put(0, attachments[Attachment.COLOR]!!.imageView.handle)
            frameBuffer = VkFrameBuffer(device, width, height, 1, pImageViews, renderPass!!.handle)
        }
    }

    val imageView: VkImageView
        get() = guiOverlayFbo!!.getAttachmentImageView(FrameBufferObject.Attachment.COLOR)

    override fun shutdown() {
        super.shutdown()
        signalSemaphore!!.destroy()
        fontsImageBundle!!.destroy()
        panelMeshBuffer!!.shutdown()
        guiPrimaryCmdBuffer!!.destroy()
        underlayImageCmdBuffer!!.destroy()
        underlayImagePipeline!!.destroy()
        underlayImageDescriptorSet!!.destroy()
        underlayImageDescriptorSetLayout!!.destroy()
        underlayImageSampler!!.destroy()
    }
}