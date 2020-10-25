package org.oreon.vk.components.ui

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.oreon.common.ui.UITextPanel
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
import org.oreon.core.vk.scenegraph.VkRenderInfo
import org.oreon.core.vk.util.VkUtil
import org.oreon.core.vk.wrapper.buffer.VkBufferHelper
import org.oreon.core.vk.wrapper.buffer.VkUniformBuffer
import org.oreon.core.vk.wrapper.command.SecondaryDrawIndexedCmdBuffer
import org.oreon.core.vk.wrapper.pipeline.GraphicsPipelineAlphaBlend
import java.util.*

class VkDynamicTextPanel(text: String, xPos: Int, yPos: Int, xScaling: Int, yScaling: Int,
                         fontsImageView: VkImageView, fontsSampler: VkSampler, fbo: VkFrameBufferObject) : UITextPanel(text, xPos, yPos, xScaling, yScaling) {
    private val graphicsPipeline: VkPipeline
    private val cmdBuffer: CommandBuffer
    private val submitInfo: SubmitInfo
    private val buffer: VkUniformBuffer
    override fun update(newText: String?) {
        if (outputText == newText) {
            return
        }
        super.update(newText)
        val ubo = MemoryUtil.memAlloc(java.lang.Float.BYTES * panel.vertices.size * 4)
        for (i in 0 until panel.vertices.size) {
            ubo.putFloat(panel.vertices[i].uvCoord.x)
            ubo.putFloat(panel.vertices[i].uvCoord.y)
            ubo.putFloat(0f)
            ubo.putFloat(0f)
        }
        ubo.flip()
        buffer.mapMemory(ubo)
    }

    init {

        // flip y-axxis for vulkan coordinate system
        orthographicMatrix!![1, 1] = -orthographicMatrix!![1, 1]
        val deviceBundle = deviceManager.getDeviceBundle(DeviceType.MAJOR_GRAPHICS_DEVICE)
        val device = deviceBundle!!.logicalDevice
        val descriptorPool = device.getDescriptorPool(Thread.currentThread().id)
        val memoryProperties = deviceManager.getPhysicalDevice(DeviceType.MAJOR_GRAPHICS_DEVICE).memoryProperties
        val shaderPipeline = ShaderPipeline(device.handle)
        shaderPipeline.createVertexShader("shaders/ui/dynamicTextPanel.vert.spv")
        shaderPipeline.createFragmentShader("shaders/ui/textPanel.frag.spv")
        shaderPipeline.createShaderPipeline()
        val pushConstantRange = java.lang.Float.BYTES * 16
        val pushConstants = MemoryUtil.memAlloc(pushConstantRange)
        pushConstants.put(BufferUtil.createByteBuffer(orthographicMatrix))
        pushConstants.flip()
        val ubo = MemoryUtil.memAlloc(java.lang.Float.BYTES * panel.vertices.size * 4)
        for (i in 0 until panel.vertices.size) {
            ubo.putFloat(panel.vertices[i].uvCoord.x)
            ubo.putFloat(panel.vertices[i].uvCoord.y)
            ubo.putFloat(0f)
            ubo.putFloat(0f)
        }
        ubo.flip()
        buffer = VkUniformBuffer(device.handle, memoryProperties, ubo)
        val descriptorSetLayout = DescriptorSetLayout(device.handle, 2)
        descriptorSetLayout.addLayoutBinding(0, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        descriptorSetLayout.addLayoutBinding(1, VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
                VK10.VK_SHADER_STAGE_VERTEX_BIT)
        descriptorSetLayout.create()
        val descriptorSet = DescriptorSet(device.handle,
                descriptorPool!!.handle,
                descriptorSetLayout.handlePointer)
        descriptorSet.updateDescriptorImageBuffer(fontsImageView.handle,
                VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, fontsSampler.handle,
                0, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
        descriptorSet.updateDescriptorBuffer(buffer.handle,
                java.lang.Float.BYTES * panel.vertices.size * 2.toLong(), 0, 1, VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
        val descriptorSets: MutableList<DescriptorSet> = ArrayList()
        val descriptorSetLayouts: MutableList<DescriptorSetLayout> = ArrayList()
        descriptorSets.add(descriptorSet)
        descriptorSetLayouts.add(descriptorSetLayout)
        val vertexInput = VkVertexInput(VertexLayout.POS2D)
        val vertexBuffer = BufferUtil.createByteBuffer(panel.vertices, VertexLayout.POS2D)
        val indexBuffer = BufferUtil.createByteBuffer(*panel.indices)
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
        graphicsPipeline = GraphicsPipelineAlphaBlend(device.handle,
                shaderPipeline, vertexInput, VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
                VkUtil.createLongBuffer(descriptorSetLayouts),
                fbo.width, fbo.height,
                fbo.renderPass!!.handle,
                fbo.colorAttachmentCount,
                1, pushConstantRange,
                VK10.VK_SHADER_STAGE_VERTEX_BIT or VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        cmdBuffer = SecondaryDrawIndexedCmdBuffer(
                device.handle,
                deviceBundle.logicalDevice.getGraphicsCommandPool(Thread.currentThread().id)!!.handle,
                graphicsPipeline.handle, graphicsPipeline.layoutHandle,
                fbo.frameBuffer!!.handle,
                fbo.renderPass!!.handle,
                0,
                VkUtil.createLongArray(descriptorSets),
                vertexBufferObject.handle, indexBufferObject.handle,
                panel.indices.size,
                pushConstants,
                VK10.VK_SHADER_STAGE_VERTEX_BIT or VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        submitInfo = SubmitInfo()
        submitInfo.setCommandBuffers(cmdBuffer.handlePointer)

		val mainRenderInfo = VkRenderInfo(commandBuffer = cmdBuffer)
		addComponent(NodeComponentType.MAIN_RENDERINFO, mainRenderInfo);
    }
}