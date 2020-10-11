package org.oreon.vk.components.ui

import org.oreon.core.vk.context.VkContext.deviceManager
import org.oreon.core.vk.scenegraph.VkMeshData
import org.oreon.core.vk.framebuffer.VkFrameBufferObject
import org.oreon.common.ui.UIElement
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.command.SubmitInfo
import org.oreon.core.vk.context.DeviceManager.DeviceType
import org.oreon.core.vk.pipeline.ShaderPipeline
import org.lwjgl.system.MemoryUtil
import org.oreon.core.vk.pipeline.VkVertexInput
import org.oreon.core.model.Vertex.VertexLayout
import org.oreon.core.vk.wrapper.pipeline.GraphicsPipelineAlphaBlend
import org.lwjgl.vulkan.VK10
import org.oreon.core.math.Vec4f
import org.oreon.core.scenegraph.NodeComponentType
import org.oreon.core.util.BufferUtil
import org.oreon.core.vk.scenegraph.VkRenderInfo
import org.oreon.core.vk.wrapper.command.SecondaryDrawIndexedCmdBuffer

class VkColorPanel(rgba: Vec4f, xPos: Int, yPos: Int, xScaling: Int, yScaling: Int,
                   panelMeshBuffer: VkMeshData, fbo: VkFrameBufferObject) : UIElement(xPos, yPos, xScaling, yScaling) {
    private val graphicsPipeline: VkPipeline
    private val cmdBuffer: CommandBuffer
    private val submitInfo: SubmitInfo

    init {

        // flip y-axxis for vulkan coordinate system
        orthographicMatrix!![1, 1] = -orthographicMatrix!![1, 1]
        val deviceBundle = deviceManager.getDeviceBundle(DeviceType.MAJOR_GRAPHICS_DEVICE)
        val device = deviceBundle!!.logicalDevice
        val shaderPipeline = ShaderPipeline(device.handle)
        shaderPipeline.createVertexShader("shaders/ui/colorPanel.vert.spv")
        shaderPipeline.createFragmentShader("shaders/ui/colorPanel.frag.spv")
        shaderPipeline.createShaderPipeline()
        val pushConstantRange = java.lang.Float.BYTES * 20
        val pushConstants = MemoryUtil.memAlloc(pushConstantRange)
        pushConstants.put(BufferUtil.createByteBuffer(orthographicMatrix))
        pushConstants.putFloat(rgba.x)
        pushConstants.putFloat(rgba.y)
        pushConstants.putFloat(rgba.z)
        pushConstants.putFloat(rgba.w)
        pushConstants.flip()
        val vertexInput = VkVertexInput(VertexLayout.POS2D)
        graphicsPipeline = GraphicsPipelineAlphaBlend(device.handle,
                shaderPipeline, vertexInput, VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
                null,
                fbo.width, fbo.height,
                fbo.renderPass!!.handle,
                fbo.colorAttachmentCount,
                1, pushConstantRange,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT or VK10.VK_SHADER_STAGE_VERTEX_BIT)
        cmdBuffer = SecondaryDrawIndexedCmdBuffer(
                device.handle,
                deviceBundle.logicalDevice.getGraphicsCommandPool(Thread.currentThread().id)!!.handle,
                graphicsPipeline.handle, graphicsPipeline.layoutHandle,
                fbo.frameBuffer!!.handle,
                fbo.renderPass!!.handle,
                0,
                null,
                panelMeshBuffer.vertexBufferObject.handle,
                panelMeshBuffer.indexBufferObject.handle,
                panelMeshBuffer.indexCount,
                pushConstants,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT or VK10.VK_SHADER_STAGE_VERTEX_BIT)
        submitInfo = SubmitInfo()
        submitInfo.setCommandBuffers(cmdBuffer.handlePointer)

		val mainRenderInfo = VkRenderInfo(commandBuffer = cmdBuffer)
		addComponent(NodeComponentType.MAIN_RENDERINFO, mainRenderInfo)
    }
}