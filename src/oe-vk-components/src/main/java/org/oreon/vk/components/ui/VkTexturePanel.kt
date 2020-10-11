package org.oreon.vk.components.ui

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.oreon.common.ui.UIElement
import org.oreon.core.model.Vertex.VertexLayout
import org.oreon.core.scenegraph.NodeComponentType
import org.oreon.core.util.BufferUtil
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.command.SubmitInfo
import org.oreon.core.vk.context.DeviceManager.DeviceType
import org.oreon.core.vk.context.VkContext.deviceManager
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import org.oreon.core.vk.framebuffer.VkFrameBufferObject
import org.oreon.core.vk.image.VkImageView
import org.oreon.core.vk.image.VkSampler
import org.oreon.core.vk.pipeline.ShaderPipeline
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.pipeline.VkVertexInput
import org.oreon.core.vk.scenegraph.VkMeshData
import org.oreon.core.vk.scenegraph.VkRenderInfo
import org.oreon.core.vk.util.VkUtil
import org.oreon.core.vk.wrapper.command.SecondaryDrawIndexedCmdBuffer
import org.oreon.core.vk.wrapper.image.VkImageBundle
import org.oreon.core.vk.wrapper.image.VkImageHelper
import org.oreon.core.vk.wrapper.pipeline.GraphicsPipelineAlphaBlend
import java.util.*

class VkTexturePanel(imageFile: String?, xPos: Int, yPos: Int, xScaling: Int, yScaling: Int,
                     panelMeshBuffer: VkMeshData, fbo: VkFrameBufferObject) : UIElement(xPos, yPos, xScaling, yScaling) {
    private val graphicsPipeline: VkPipeline
    private val cmdBuffer: CommandBuffer
    private val submitInfo: SubmitInfo
    private val imageBundle: VkImageBundle

    init {

        // flip y-axxis for vulkan coordinate system
        orthographicMatrix!![1, 1] = -orthographicMatrix!![1, 1]
        val deviceBundle = deviceManager.getDeviceBundle(DeviceType.MAJOR_GRAPHICS_DEVICE)
        val device = deviceBundle!!.logicalDevice
        val descriptorPool = device.getDescriptorPool(Thread.currentThread().id)
        val memoryProperties = deviceManager.getPhysicalDevice(DeviceType.MAJOR_GRAPHICS_DEVICE).memoryProperties
        val fontsImage = VkImageHelper.loadImageFromFile(
                device.handle, memoryProperties,
                device.getTransferCommandPool(Thread.currentThread().id)!!.handle,
                device.transferQueue,
                imageFile,
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
        imageBundle = VkImageBundle(fontsImage, fontsImageView, sampler)
        val shaderPipeline = ShaderPipeline(device.handle)
        shaderPipeline.createVertexShader("shaders/ui/texturePanel.vert.spv")
        shaderPipeline.createFragmentShader("shaders/ui/texturePanel.frag.spv")
        shaderPipeline.createShaderPipeline()
        val pushConstantRange = java.lang.Float.BYTES * 16
        val pushConstants = MemoryUtil.memAlloc(pushConstantRange)
        pushConstants.put(BufferUtil.createByteBuffer(orthographicMatrix))
        pushConstants.flip()
        val vertexInput = VkVertexInput(VertexLayout.POS2D)
        val descriptorSetLayout = DescriptorSetLayout(device.handle, 1)
        descriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        descriptorSetLayout.create()
        val descriptorSet = DescriptorSet(device.handle,
                descriptorPool!!.handle,
                descriptorSetLayout.handlePointer)
        descriptorSet.updateDescriptorImageBuffer(imageBundle.imageView.handle,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, imageBundle.sampler.handle,
                0, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        val descriptorSets: MutableList<DescriptorSet> = ArrayList()
        val descriptorSetLayouts: MutableList<DescriptorSetLayout> = ArrayList()
        descriptorSets.add(descriptorSet)
        descriptorSetLayouts.add(descriptorSetLayout)
        graphicsPipeline = GraphicsPipelineAlphaBlend(device.handle,
                shaderPipeline, vertexInput, VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
                VkUtil.createLongBuffer(descriptorSetLayouts),
                fbo.width, fbo.height,
                fbo.renderPass!!.handle,
                fbo.colorAttachmentCount,
                1, pushConstantRange,
                VK10.VK_SHADER_STAGE_VERTEX_BIT)
        cmdBuffer = SecondaryDrawIndexedCmdBuffer(
                device.handle,
                deviceBundle.logicalDevice.getGraphicsCommandPool(Thread.currentThread().id)!!.handle,
                graphicsPipeline.handle, graphicsPipeline.layoutHandle,
                fbo.frameBuffer!!.handle,
                fbo.renderPass!!.handle,
                0,
                VkUtil.createLongArray(descriptorSets),
                panelMeshBuffer.vertexBufferObject.handle,
                panelMeshBuffer.indexBufferObject.handle,
                panelMeshBuffer.indexCount,
                pushConstants,
                VK10.VK_SHADER_STAGE_VERTEX_BIT)
        submitInfo = SubmitInfo()
        submitInfo.setCommandBuffers(cmdBuffer.handlePointer)

        val mainRenderInfo = VkRenderInfo(commandBuffer = cmdBuffer)
		addComponent(NodeComponentType.MAIN_RENDERINFO, mainRenderInfo);
    }
}